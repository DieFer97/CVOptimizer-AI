#* @filter cors
cors <- function(req, res) {
    res$setHeader("Access-Control-Allow-Origin", "*")
    res$setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
    res$setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
    res$setHeader("Access-Control-Max-Age", "86400")
    plumber::forward()
}

#* @filter logger
function(req) {
    cat(paste(Sys.time(), req$REQUEST_METHOD, req$PATH_INFO, "\n"))
    plumber::forward()
}

suppressMessages({
    library(plumber)
    library(pdftools)
    library(docxtractr)
    library(tm)
    library(randomForest)
    library(caret)
    library(text2vec)
    library(readr)
    library(dplyr)
    library(jsonlite)
})

CONFIG <- list(
    max_file_size = 10 * 1024 * 1024,
    supported_formats = c("pdf", "docx", "txt"),
    model_confidence_threshold = 0.5
)

initialize_model <- function() {
    tryCatch(
        {
            cv_data_file <- "cv_samples/cv_data.csv"
            if (!file.exists(cv_data_file)) {
                stop("Archivo de datos de entrenamiento no encontrado: ", cv_data_file)
            }
            cv_data <- read_csv(cv_data_file, col_types = cols(texto_cv = "c", area = "c"))
            if (nrow(cv_data) == 0) {
                stop("El archivo de datos está vacío")
            }

            prep_fun <- function(texto) {
                texto <- tolower(texto)
                texto <- removePunctuation(texto)
                texto <- removeNumbers(texto)
                texto <- removeWords(texto, stopwords("spanish"))
                texto <- stripWhitespace(texto)
                return(texto)
            }
            tok_fun <- word_tokenizer
            it_train <- itoken(cv_data$texto_cv, preprocessor = prep_fun, tokenizer = tok_fun)
            vocab <- create_vocabulary(it_train, ngram = c(1L, 3L))
            vocab <- prune_vocabulary(vocab, term_count_min = 2)
            vectorizer <- vocab_vectorizer(vocab)
            dtm <- create_dtm(it_train, vectorizer)
            dtm_matrix <- as.matrix(dtm)

            set.seed(123)
            train_control <- trainControl(method = "cv", number = 5)
            modelo <- train(
                x = dtm_matrix,
                y = as.factor(cv_data$area),
                method = "rf",
                trControl = train_control,
                tuneLength = 10
            )

            return(list(
                modelo = modelo,
                vectorizer = vectorizer,
                vocab = vocab,
                areas = unique(cv_data$area),
                status = "success"
            ))
        },
        error = function(e) {
            return(list(
                modelo = NULL,
                error = as.character(e),
                status = "error"
            ))
        }
    )
}

MODEL_DATA <- initialize_model()

#* Endpoint de estado del sistema
#* @get /health
#* @serializer unboxedJSON
function() {
    system_info <- list( # nolint
        success = TRUE, # nolint
        status = if (MODEL_DATA$status == "success") "operational" else "error",
        timestamp = format(Sys.time(), "%Y-%m-%d %H:%M:%S"),
        version = "1.0.8",
        model_status = MODEL_DATA$status,
        supported_formats = CONFIG$supported_formats,
        max_file_size_mb = CONFIG$max_file_size / (1024 * 1024)
    ) # nolint
    if (MODEL_DATA$status == "error") {
        system_info$error <- MODEL_DATA$error # nolint
    } # nolint
    return(system_info) # nolint
}

#* Obtener información del modelo y áreas disponibles
#* @get /model/info
#* @serializer unboxedJSON
function() {
    if (MODEL_DATA$status != "success") {
        return(list(
            success = FALSE,
            error = "Modelo no disponible",
            details = MODEL_DATA$error
        ))
    }
    return(list(
        success = TRUE,
        areas_disponibles = MODEL_DATA$areas,
        total_areas = length(MODEL_DATA$areas),
        vocabulario_size = nrow(MODEL_DATA$vocab),
        modelo_tipo = "Random Forest",
        configuracion = CONFIG
    ))
}

#* Procesar archivo CV
#* @param file:file Archivo CV (PDF, DOCX, TXT)
#* @post /analyze/file
#* @serializer unboxedJSON
function(req, file) {
    cat("Verifying received file:\n")
    cat("- file is NULL:", is.null(file), "\n")
    if (!is.null(file)) {
        cat("- Names in file:", paste(names(file), collapse = ", "), "\n")
        cat("- Has data:", !is.null(file[[1]]), "\n")
        cat("- Has filename:", !is.null(names(file)[1]), "\n")
        if (!is.null(file[[1]])) {
            cat("- Data size:", length(file[[1]]), "bytes\n")
        }
    }

    if (MODEL_DATA$status != "success") {
        return(create_error_response("Model not available", MODEL_DATA$error))
    }

    if (is.null(file) || length(file) == 0) {
        return(create_error_response("No file received", "File required"))
    }

    filename <- names(file)[1]
    file_data <- file[[1]]

    if (is.null(filename) || is.null(file_data)) {
        return(create_error_response("Invalid file structure", "Could not extract file data"))
    }

    if (length(file_data) > CONFIG$max_file_size) {
        return(create_error_response(
            "File too large",
            paste("Maximum allowed:", CONFIG$max_file_size / (1024 * 1024), "MB")
        ))
    }

    file_ext <- tolower(tools::file_ext(filename))
    if (!file_ext %in% CONFIG$supported_formats) {
        return(create_error_response(
            "Unsupported format",
            paste("Allowed formats:", paste(CONFIG$supported_formats, collapse = ", "))
        ))
    }

    tryCatch(
        {
            file_obj <- list(
                filename = filename,
                dataptr = file_data
            )

            temp_file <- create_temp_file_corrected(file_obj)
            on.exit(unlink(temp_file))

            texto <- extract_text_from_file(temp_file, file_ext)

            if (is.null(texto) || nchar(trimws(texto)) == 0) {
                return(create_error_response("Empty text", "Could not extract content from file"))
            }

            resultado <- analyze_text_content(texto)
            resultado$archivo_info <- list(
                nombre = filename,
                tamano_bytes = length(file_data),
                formato = file_ext,
                caracteres_extraidos = nchar(texto)
            )
            return(resultado)
        },
        error = function(e) {
            return(create_error_response("Error processing file", as.character(e)))
        }
    )
}

create_temp_file_corrected <- function(file_obj) {
    temp_dir <- tempdir()
    temp_file <- file.path(temp_dir, file_obj$filename)
    writeBin(file_obj$dataptr, temp_file)
    return(temp_file)
}

extract_text_from_file <- function(file_path, extension) {
    switch(extension,
        "pdf" = {
            pdf_text(file_path) %>% paste(collapse = " ")
        },
        "docx" = {
            doc <- read_docx(file_path)
            docx_extract_all(doc) %>% paste(collapse = " ")
        },
        "txt" = {
            readLines(file_path, warn = FALSE, encoding = "UTF-8") %>% paste(collapse = " ")
        }
    )
}

analyze_text_content <- function(texto) {
    prep_fun <- function(texto) {
        texto <- tolower(texto)
        texto <- removePunctuation(texto)
        texto <- removeNumbers(texto)
        texto <- removeWords(texto, stopwords("spanish"))
        texto <- stripWhitespace(texto)
        return(texto)
    }
    tok_fun <- word_tokenizer
    it_nuevo <- itoken(texto, preprocessor = prep_fun, tokenizer = tok_fun)
    dtm_nuevo <- create_dtm(it_nuevo, MODEL_DATA$vectorizer)
    dtm_nuevo_matrix <- as.matrix(dtm_nuevo)

    prediccion <- predict(MODEL_DATA$modelo, dtm_nuevo_matrix, type = "prob")
    probabilidades <- prediccion
    resultados <- data.frame(
        area = colnames(probabilidades),
        porcentaje = floor(as.numeric(probabilidades[1, ]) * 100), # Cambiado a floor para enteros
        confianza = ifelse(as.numeric(probabilidades[1, ]) > CONFIG$model_confidence_threshold, "alta", "baja")
    ) %>%
        arrange(desc(porcentaje))
    return(list(
        success = TRUE,
        prediccion_principal = resultados$area[1],
        confianza_principal = resultados$porcentaje[1],
        todas_las_areas = resultados,
        metadatos = list(
            texto_caracteres = nchar(texto),
            timestamp = format(Sys.time(), "%Y-%m-%d %H:%M:%S"),
            modelo_version = "1.0.8"
        )
    ))
}

create_error_response <- function(mensaje, detalle = NULL) {
    response <- list(
        success = FALSE,
        error = mensaje,
        timestamp = format(Sys.time(), "%Y-%m-%d %H:%M:%S")
    )
    if (!is.null(detalle)) {
        response$details <- detalle
    }
    return(response)
}

create_temp_file <- function(file) {
    temp_dir <- tempdir()
    temp_file <- file.path(temp_dir, file$filename)
    writeBin(file$dataptr, temp_file)
    return(temp_file)
}

#* Obtener estadísticas del sistema
#* @get /stats
#* @serializer unboxedJSON
function() {
    if (MODEL_DATA$status != "success") {
        return(list(success = FALSE, error = "Modelo no disponible"))
    }
    return(list(
        success = TRUE,
        estadisticas = list(
            areas_disponibles = length(MODEL_DATA$areas),
            vocabulario_modelo = nrow(MODEL_DATA$vocab),
            configuracion_actual = CONFIG,
            tiempo_actividad = format(Sys.time(), "%Y-%m-%d %H:%M:%S")
        )
    ))
}
