package kg.teksher.gs1scanner.api

import kg.teksher.gs1scanner.model.ScanRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/scans")
    fun sendScan(
        @Body request: ScanRequest
    ): Call<Void>
}