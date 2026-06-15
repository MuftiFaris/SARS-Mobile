package com.informatika.sars.data.remote

import com.informatika.sars.data.model.RequestResponse
import com.informatika.sars.data.model.SubmitRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LaravelApiService {
    @POST("api/mahasiswa/jadwal/request")
    suspend fun submitJadwalRequest(
        @Body request: SubmitRequestDto
    ): Response<RequestResponse>
}
