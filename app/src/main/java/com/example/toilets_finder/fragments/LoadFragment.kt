package com.example.toilets_finder.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.toilets_finder.R
import com.example.toilets_finder.Supabase
import com.example.toilets_finder.Toilet
import com.example.toilets_finder.ToiletDataStore
import com.example.toilets_finder.ToiletInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class LoadFragment : Fragment() {

    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var loadingProgressText: TextView
    private var totalRequests = 1
    private var responsesReceived = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_load, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ToiletDataStore.toiletList.clear()
        responsesReceived = 0

        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        loadingProgressText = view.findViewById(R.id.loadingProgressTextView)

        loadingProgressBar.progress = 0
        loadingProgressText.text = "Loading map : 0%"
        fetchToiletsFromSupabase()

        return view
    }


    private fun fetchAllToiletsData() {
        for (i in 0 until totalRequests) {
            val apiUrl =
                "https://opendata.paris.fr/api/explore/v2.1/catalog/datasets/sanisettesparis/records?limit=100&offset=" + 100 * i
            fetchToiletsFromSupabase()
        }
    }

    private fun fetchToiletsFromSupabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = Supabase.client
                    .from("toilets")
                    .select()
                    .decodeList<ToiletInfo>()

                withContext(Dispatchers.Main) {
                    val averageRatingMap = getAverageRating()
                    response.forEach { toilet ->
                        val id = toilet.id
                        val lat = toilet.location.lat
                        val lon = toilet.location.lon
                        val address = toilet.address
                        val pmrAccess = "Accès PMR: " + if (toilet.pmrAccess == true) "Oui" else "Non"
                        val type = toilet.type
                        val imageSrc: Int = when (type) {
                            "SANISETTE", "WC PUBLICS PERMANENTS" -> R.drawable.sanisette
                            "TOILETTES" -> R.drawable.toilette
                            else -> R.drawable.urinoir
                        }
                        val openingHours = "Horaires: " + toilet.schedule
                        val averageRating = averageRatingMap.getOrDefault(id, 0.0)
                        val yourRating = 0f
                        val ficheURL = toilet.ficheUrl

                        ToiletDataStore.toiletList.add(
                            Toilet(
                                id,
                                lat,
                                lon,
                                address,
                                type,
                                imageSrc,
                                openingHours,
                                pmrAccess,
                                averageRating,
                                yourRating,
                                ficheURL
                            )
                        )
                    }

                    responsesReceived++
                    println(responsesReceived)
                    val progress = (responsesReceived * 100) / totalRequests
                    loadingProgressBar.progress = progress
                    loadingProgressText.text = "Map loading : $progress%"

                    if (responsesReceived == totalRequests) {
                        activity?.let {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fl_wrapper, MapFragment())
                                .addToBackStack(null)
                                .commit()
                        }
                        Log.d("SUPABASE_FETCH", "Loaded ${response.size} toilets from Supabase.")
                    }
                }
            } catch (e: Exception) {
                Log.e("SUPABASE_FETCH", "Failed to fetch toilets: ${e.message}")
            }
        }
    }
    suspend fun getAverageRating(): Map<String, Double> {
        return try {
            withContext(Dispatchers.IO) {
                val response = Supabase.client
                    .from("reviews")
                    .select(columns = Columns.list("toilet_id", "rating"))
                    .decodeList<ToiletRating>()

                // Calculer la moyenne des notes par toilette manuellement
                val ratingsMap = response.groupBy { it.toilet_id }
                    .mapValues { entry ->
                        val ratings = entry.value.map { it.rating }
                        if (ratings.isNotEmpty()) {
                            var sum = 0.0
                            var count = 0

                            for (rating in ratings) {
                                if (rating != null) {
                                    sum += rating
                                }
                                count++
                            }
                            sum / count
                        } else {
                            0.0
                        }
                    }
                ratingsMap
            }
        } catch (e: Exception) {
            Log.e("SUPABASE_FETCH", "Erreur pendant le calcul des moyennes des ratings: ${e.message}")
            emptyMap()
        }
    }
}

@Serializable
data class ToiletRating(
    val toilet_id: String,
    val rating: Double? = null
)
