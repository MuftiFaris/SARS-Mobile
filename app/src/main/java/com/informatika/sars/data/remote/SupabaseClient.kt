package com.informatika.sars.data.remote

import com.informatika.sars.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

import java.util.concurrent.TimeUnit

object SupabaseClient {
    // Load from BuildConfig (set in build.gradle.kts from local.properties)
    private val SUPABASE_URL = BuildConfig.SUPABASE_URL.takeIf { it.isNotEmpty() } 
        ?: "https://qazkqstymqwylpkeckyv.supabase.co/"
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY.takeIf { it.isNotEmpty() }
        ?: throw IllegalStateException("SUPABASE_KEY not configured in local.properties")

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        httpEngine = OkHttp.create()
        install(Auth) {
            autoSaveToStorage = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
        install(Realtime)
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        })
    }

    // Dynamic JWT extraction
    val currentAccessToken: String?
        get() = client.auth.currentSessionOrNull()?.accessToken

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                currentAccessToken?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }


}


// Extension to access postgrest
fun SupabaseClient.postgrestAccess() = SupabaseClient.client.postgrest