#* @filter cors
cors <- function(req, res) {
    res$setHeader("Access-Control-Allow-Origin", "*") # nolint
    res$setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS") # nolint
    res$setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With") # nolint
    res$setHeader("Access-Control-Max-Age", "86400")
    plumber::forward()
}

#* @filter logger
function(req) {
    cat(paste(Sys.time(), req$REQUEST_METHOD, req$PATH_INFO, "\n")) # nolint
    plumber::forward()
}

# Cargar librerías necesarias
suppressMessages({
    library(plumber) # nolint
    library(pdftools)
    library(docxtractr)
    library(tm)
    library(e1071)
    library(readr)
    library(dplyr)
    library(jsonlite)
})

CONFIG <- list( # nolint
    max_file_size = 10 * 1024 * 1024, # nolint
    supported_formats = c("pdf", "docx", "txt"),
    model_confidence_threshold = 0.1
)

initialize_model <- function() {
    tryCatch({ # nolint
        cv_data_file <- "cv_samples/cv_data.csv" # nolint
        if (!file.exists(cv_data_file)) { # nolint
            stop("Archivo de datos de entrenamiento no encontrado: ", cv_data_file) # nolint
        } # nolint
        cv_data <- read_csv(cv_data_file, col_types = cols(texto_cv = "c", area = "c")) # nolint
        if (nrow(cv_data) == 0) {
            stop("El archivo de datos está vacío") # nolint
        } # nolint

        corpus <- VCorpus(VectorSource(cv_data$texto_cv)) # nolint
        corpus <- tm_map(corpus, content_transformer(tolower))
        corpus <- tm_map(corpus, removePunctuation)
        corpus <- tm_map(corpus, removeNumbers)
        corpus <- tm_map(corpus, removeWords, stopwords("spanish"))
        corpus <- tm_map(corpus, stripWhitespace)
        dtm <- DocumentTermMatrix(corpus)
        dtm_matrix <- as.matrix(dtm)

        modelo <- svm(x = dtm_matrix, y = as.factor(cv_data$area), probability = TRUE) # nolint
         # nolint
        return(list( # nolint
            modelo = modelo, # nolint
            dtm_terms = Terms(dtm),
            areas = unique(cv_data$area),
            status = "success"
        )) # nolint
         # nolint
    }, error = function(e) { # nolint
        return(list( # nolint
            modelo = NULL, # nolint
            error = as.character(e),
            status = "error"
        )) # nolint
    }) # nolint
}

MODEL_DATA <- initialize_model() # nolint

#* Endpoint de estado del sistema
#* @get /health
#* @serializer json
function() {
    system_info <- list( # nolint
        status = if(MODEL_DATA$status == "success") "operational" else "error", # nolint
        timestamp = format(Sys.time(), "%Y-%m-%d %H:%M:%S"),
        version = "1.0.0",
        model_status = MODEL_DATA$status,
        supported_formats = CONFIG$supported_formats,
        max_file_size_mb = CONFIG$max_file_size / (1024 * 1024)
    ) # nolint
     # nolint
    if (MODEL_DATA$status == "error") { # nolint
        system_info$error <- MODEL_DATA$error # nolint
    } # nolint
     # nolint
    return(system_info) # nolint
}

#* Obtener información del modelo y áreas disponibles
#* @get /model/info
#* @serializer json
function() {
    if (MODEL_DATA$status != "success") { # nolint
        return(list( # nolint
            success = FALSE, # nolint
            error = "Modelo no disponible",
            details = MODEL_DATA$error
        )) # nolint
    } # nolint
     # nolint
    return(list( # nolint
        success = TRUE, # nolint
        areas_disponibles = MODEL_DATA$areas,
        total_areas = length(MODEL_DATA$areas),
        vocabulario_size = length(MODEL_DATA$dtm_terms),
        modelo_tipo = "Support Vector Machine",
        configuracion = CONFIG
    )) # nolint
}

#* Procesar archivo CV
#* @param archivo:file Archivo CV (PDF, DOCX, TXT)
#* @post /analyze/file
#* @serializer json
function(req, archivo) {

    if (MODEL_DATA$status != "success") { # nolint
        return(create_error_response("Modelo no disponible", MODEL_DATA$error)) # nolint
    } # nolint

    if (is.null(archivo) || is.null(archivo$data)) { # nolint
        return(create_error_response("No se recibió archivo", "Archivo requerido")) # nolint # nolint
    } # nolint
    # Validar tamaño
    if (length(archivo$data) > CONFIG$max_file_size) { # nolint
        return(create_error_response("Archivo demasiado grande",  # nolint
                                paste("Máximo permitido:", CONFIG$max_file_size / (1024*1024), "MB"))) # nolint
    } # nolint


    file_ext <- tolower(tools::file_ext(archivo$filename)) # nolint
    if (!file_ext %in% CONFIG$supported_formats) {
        return(create_error_response("Formato no soportado",  # nolint
                                paste("Formatos permitidos:", paste(CONFIG$supported_formats, collapse = ", ")))) # nolint
    } # nolint

    tryCatch({ # nolint

        temp_file <- create_temp_file(archivo) # nolint
        on.exit(unlink(temp_file))

        texto <- extract_text_from_file(temp_file, file_ext) # nolint

        if (is.null(texto) || nchar(trimws(texto)) == 0) { # nolint
            return(create_error_response("Texto vacío", "No se pudo extraer contenido del archivo")) # nolint
        } # nolint
        resultado <- analyze_text_content(texto)
        resultado$archivo_info <- list(
            nombre = archivo$filename, # nolint
            tamano_bytes = length(archivo$data),
            formato = file_ext,
            caracteres_extraidos = nchar(texto)
        ) # nolint
        return(resultado)
    }, error = function(e) { # nolint
        return(create_error_response("Error procesando archivo", as.character(e))) # nolint
    }) # nolint
}

#* Procesar texto directo
#* @param texto Contenido textual del CV
#* @post /analyze/text
#* @serializer json
function(texto) {
    if (MODEL_DATA$status != "success") { # nolint
        return(create_error_response("Modelo no disponible", MODEL_DATA$error)) # nolint
    } # nolint

    if (is.null(texto) || nchar(trimws(texto)) == 0) { # nolint
        return(create_error_response("Texto vacío", "Contenido textual requerido")) # nolint
    } # nolint

    tryCatch({ # nolint
        return(analyze_text_content(texto)) # nolint
    }, error = function(e) { # nolint
        return(create_error_response("Error procesando texto", as.character(e))) # nolint
    }) # nolint
}

#* Obtener estadísticas del sistema
#* @get /stats
#* @serializer json
function() {
    if (MODEL_DATA$status != "success") { # nolint
        return(list(success = FALSE, error = "Modelo no disponible")) # nolint
    } # nolint

    return(list( # nolint
        success = True, # nolint
        estadisticas = list(
            areas_disponibles = length(MODEL_DATA$areas), # nolint
            vocabulario_modelo = length(MODEL_DATA$dtm_terms),
            configuracion_actual = CONFIG,
            tiempo_actividad = format(Sys.time(), "%Y-%m-%d %H:%M:%S")
        ) # nolint
    )) # nolint
}


create_error_response <- function(mensaje, detalle = NULL) {
    response <- list( # nolint
        success = FALSE, # nolint
        error = mensaje,
        timestamp = format(Sys.time(), "%Y-%m-%d %H:%M:%S")
    ) # nolint

    if (!is.null(detalle)) { # nolint
        response$details <- detalle # nolint
    } # nolint

    return(response) # nolint
}

create_temp_file <- function(archivo) {
    temp_dir <- tempdir() # nolint
    temp_file <- file.path(temp_dir, archivo$filename)
    writeBin(archivo$data, temp_file)
    return(temp_file) # nolint
}

extract_text_from_file <- function(file_path, extension) {
    switch(extension, # nolint
        "pdf" = { # nolint
            pdf_text(file_path) %>% paste(collapse = " ") # nolint
        }, # nolint
        "docx" = {
            doc <- read_docx(file_path) # nolint
            docx_extract_all(doc) %>% paste(collapse = " ")
        }, # nolint
        "txt" = {
            readLines(file_path, warn = FALSE, encoding = "UTF-8") %>% paste(collapse = " ") # nolint
        } # nolint
    ) # nolint
}

analyze_text_content <- function(texto) {
    corpus_nuevo <- VCorpus(VectorSource(texto)) # nolint
    corpus_nuevo <- tm_map(corpus_nuevo, content_transformer(tolower))
    corpus_nuevo <- tm_map(corpus_nuevo, removePunctuation)
    corpus_nuevo <- tm_map(corpus_nuevo, removeNumbers)
    corpus_nuevo <- tm_map(corpus_nuevo, removeWords, stopwords("spanish"))
    corpus_nuevo <- tm_map(corpus_nuevo, stripWhitespace)

    dtm_nuevo <- DocumentTermMatrix(corpus_nuevo, control = list(dictionary = MODEL_DATA$dtm_terms)) # nolint
    dtm_nuevo_matrix <- as.matrix(dtm_nuevo) # nolint

    prediccion <- predict(MODEL_DATA$modelo, dtm_nuevo_matrix, probability = TRUE) # nolint
    probabilidades <- attr(prediccion, "probabilities")
    resultados <- data.frame(
        area = colnames(probabilidades), # nolint
        porcentaje = round(as.vector(probabilidades) * 100, 2), # nolint
        confianza = ifelse(as.vector(probabilidades) > CONFIG$model_confidence_threshold, "alta", "baja") # nolint
    ) %>%  # nolint
    arrange(desc(porcentaje)) # nolint
    return(list( # nolint # nolint
        success = TRUE, # nolint
        prediccion_principal = resultados$area[1],
        confianza_principal = resultados$porcentaje[1],
        todas_las_areas = resultados,
        metadatos = list(
            texto_caracteres = nchar(texto), # nolint
            timestamp = format(Sys.time(), "%Y-%m-%d %H:%M:%S"),
            modelo_version = "1.0.0"
        ) # nolint
    )) # nolint
}