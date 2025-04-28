package com.example.toilets_finder

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.*
import kotlinx.serialization.json.Json
import io.github.jan.supabase.serializer.KotlinXSerializer

object Supabase {
    lateinit var client: SupabaseClient

    fun init() {
        client = createSupabaseClient(
            supabaseUrl = "https://zxhcldwajyevprifqwya.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp4aGNsZHdhanlldnByaWZxd3lhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQ3ODc3NDIsImV4cCI6MjA2MDM2Mzc0Mn0.UGSc0Y3oDPWqpd8RrQFZAlphG3dvjz7iTxdKxtV0u0E"
        )
        {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })

            install(Postgrest)
            install(Auth)

            httpEngine = OkHttp.create()
        }
    }
}