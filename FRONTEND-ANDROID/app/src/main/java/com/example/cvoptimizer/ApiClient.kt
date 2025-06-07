import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://192.168.137.1:8000/" // Asegúrate de que esta URL sea correcta

    val apiService: ApiService by lazy {
        // Configurar interceptor de registro
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Configurar cliente OkHttpClient con tiempo de espera
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // Tiempo de espera para establecer conexión
            .readTimeout(30, TimeUnit.SECONDS)     // Tiempo de espera para leer respuesta
            .writeTimeout(30, TimeUnit.SECONDS)    // Tiempo de espera para escribir solicitud
            .build()

        // Configurar Retrofit
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}