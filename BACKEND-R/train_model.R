library(text2vec)
library(randomForest)
library(caret)
library(tm)

datos <- read.csv("cv_samples/cv_data.csv", stringsAsFactors = FALSE, encoding = "UTF-8") # nolint

prep_fun <- function(texto) {
  texto <- tolower(texto)
  texto <- removePunctuation(texto)
  texto <- removeWords(texto, stopwords("es"))
  return(texto) # nolint
}

tok_fun <- word_tokenizer

it_train <- itoken(datos$texto_cv, preprocessor = prep_fun, tokenizer = tok_fun)

vocab <- create_vocabulary(it_train)
vocab <- prune_vocabulary(vocab, term_count_min = 2)
vectorizer <- vocab_vectorizer(vocab)

dtm_train <- create_dtm(it_train, vectorizer)
dtm_matrix <- as.matrix(dtm_train)

set.seed(123)
train_control <- trainControl(method = "cv", number = 5)

modelo <- train(
  x = dtm_matrix,
  y = as.factor(datos$area),
  method = "rf",
  trControl = train_control,
  tuneLength = 5
)

saveRDS(modelo, "modelo_cv.rds")
saveRDS(vectorizer, "vectorizer.rds")
saveRDS(vocab, "vocabulario.rds")

cat("Modelo entrenado exitosamente\n")
cat("Accuracy promedio (CV):", mean(modelo$resample$Accuracy), "\n")
print(table(datos$area))
