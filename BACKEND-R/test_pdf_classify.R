library(plumber)
library(pdftools)
library(tm)
library(e1071)
library(jsonlite)
library(text2vec)

modelo <- readRDS("modelo_cv.rds")
vectorizer <- readRDS("vectorizer.rds")

prep_fun <- function(texto) {
  texto <- tolower(texto)
  texto <- removePunctuation(texto)
  texto <- removeWords(texto, stopwords("es"))
  return(texto) # nolint
}

tok_fun <- word_tokenizer

extract_text_from_pdf <- function(pdf_path) {
  texto_pdf <- pdf_text(pdf_path)
  texto_completo <- paste(texto_pdf, collapse = " ")
  return(texto_completo) # nolint
}

predict_cv_area <- function(texto_completo) {
  it <- itoken(texto_completo, preprocessor = prep_fun, tokenizer = tok_fun)
  dtm <- create_dtm(it, vectorizer)
  dtm_matrix <- as.matrix(dtm)

  probs <- predict(modelo, dtm_matrix, probability = TRUE)
  porcentajes <- round(attr(probs, "probabilities") * 100, 1)

  resultados <- data.frame(
    area = colnames(porcentajes),
    porcentaje = as.numeric(porcentajes[1, ])
  )
  resultados <- resultados[order(resultados$porcentaje, decreasing = TRUE), ]

  return(resultados) # nolint
}

#* @apiTitle CV Analyzer API
#* @apiDescription API para analizar CV y predecir áreas de trabajo

#* Subir CV y obtener análisis
#* @post /predict
function(req, res, file) {
  temp_dir <- tempdir()
  temp_file <- file.path(temp_dir, file$filename)
  writeBin(file$dataptr, temp_file)

  texto_completo <- extract_text_from_pdf(temp_file)

  resultados <- predict_cv_area(texto_completo)

  unlink(temp_file)

  res$setContent-Type("application/json") # nolint
  return(toJSON(list(resultados = resultados, error = NULL), auto_unbox = TRUE)) # nolint
}

#* @get /__docs__
function() {
  swagger-ui() # nolint
}