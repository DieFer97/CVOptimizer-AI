library(plumber)
library(pdftools)
library(tm)
library(text2vec)
library(jsonlite)

modelo <- readRDS("modelo_cv.rds")
vectorizer <- readRDS("vectorizer.rds")

prep_fun <- function(texto) {
  texto <- tolower(texto)
  texto <- removePunctuation(texto)
  texto <- removeNumbers(texto)
  texto <- removeWords(texto, stopwords("es"))
  texto <- stripWhitespace(texto)
  return(texto)
}

tok_fun <- word_tokenizer

extract_text_from_pdf <- function(pdf_path) {
  texto_pdf <- pdf_text(pdf_path)
  texto_completo <- paste(texto_pdf, collapse = " ")
  return(texto_completo)
}

predict_cv_area <- function(texto_completo) {
  it <- itoken(texto_completo, preprocessor = prep_fun, tokenizer = tok_fun)
  dtm <- create_dtm(it, vectorizer)
  dtm_matrix <- as.matrix(dtm)

  probs <- predict(modelo, dtm_matrix, type = "prob")
  porcentajes <- floor(as.numeric(probs[1, ]) * 100) # Cambiado a floor para enteros

  resultados <- data.frame(
    area = colnames(probs),
    porcentaje = porcentajes
  )
  resultados <- resultados[order(resultados$porcentaje, decreasing = TRUE), ]

  return(resultados)
}

#* @apiTitle CV Analyzer API
#* @apiDescription API para analizar CV y predecir áreas de trabajo

#* Subir CV y obtener análisis
#* @post /predict
#* @serializer unboxedJSON
function(req, res, file) {
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

  if (is.null(file) || length(file) == 0) {
    res$status <- 400
    return(list(error = "No file received"))
  }

  filename <- names(file)[1]
  file_data <- file[[1]]

  if (is.null(filename) || is.null(file_data)) {
    res$status <- 400
    return(list(error = "Invalid file structure"))
  }

  temp_dir <- tempdir()
  temp_file <- file.path(temp_dir, filename)
  writeBin(file_data, temp_file)

  texto_completo <- extract_text_from_pdf(temp_file)

  resultados <- predict_cv_area(texto_completo)

  unlink(temp_file)

  res$setHeader("Content-Type", "application/json")
  return(list(resultados = resultados, error = NULL))
}

#* @get /__docs__
#* @serializer unboxedJSON
function() {
  swagger_ui()
}
