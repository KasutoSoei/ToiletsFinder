package com.example.toilets_finder

import io.github.jan.supabase.postgrest.from
import android.util.Log
import kotlinx.coroutines.*

//send the data of the overlay to the database(supa base)
private fun sendToiletsBatchToSupabase(toilets: List<ToiletInfo>) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val chunkSize = 100
            for (chunk in toilets.chunked(chunkSize)) {
                try {
                    Supabase.client
                        .from("toilets")
                        .upsert(chunk) {
                             //onConflict = "location"
                            // ignoreDuplicates = true
                            select()
                        }
                    Log.d("Supabase", "Inserted batch of ${chunk.size} toilets.")
                    delay(500) // respect API rate limits
                } catch (e: Exception) {
                    Log.e("Supabase", "Batch insert failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Erreur d’enregistrement: ${e.message}")
        }
    }
}



