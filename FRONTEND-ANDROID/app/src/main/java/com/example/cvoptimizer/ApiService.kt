import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import com.example.cvoptimizer.ApiResponse

interface ApiService {
    @Multipart
    @POST("analyze/file")
    fun uploadCV(@Part file: MultipartBody.Part):
            Call<ApiResponse>
}