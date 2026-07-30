package kg.teksher.gs1scanner.network

import kg.teksher.gs1scanner.api.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

     //Для эмулятора Android Studio
     //private const val BASE_URL = "http://10.0.2.2:8080/"

     //Для реального телефона:
     //private const val BASE_URL = "http://172.20.10.2:8080/"

     //Внешний сайт
     //private const val BASE_URL = "https://teksher-api.onrender.com/"
// Нужно поменять если надо
    private const val BASE_URL ="main.teksher.kg"
    val api: ApiService by lazy {

        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}