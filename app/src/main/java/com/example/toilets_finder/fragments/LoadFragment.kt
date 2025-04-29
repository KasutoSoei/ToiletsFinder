package com.example.toilets_finder.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.toilets_finder.R
import com.example.toilets_finder.Supabase
import com.example.toilets_finder.Toilet
import com.example.toilets_finder.ToiletDataStore
import com.example.toilets_finder.ToiletInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
                        val averageRating = 3.7f
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


//    private fun fetchToiletData(apiUrl: String) {
//                    CoroutineScope(Dispatchers.IO).launch {
//                        try {
//                            val url = URL(apiUrl)
//                            val connection = url.openConnection() as HttpURLConnection
//                            connection.requestMethod = "GET"
//
//                            if (connection.responseCode == 200) {
//                                val response =
//                                    connection.inputStream.bufferedReader().use { it.readText() }
//                                val jsonResponse = JSONObject(response)
//                                val records = jsonResponse.getJSONArray("results")
//
//                                for (i in 0 until records.length()) {
//                                    val record = records.getJSONObject(i)
//
//                                    if (!record.isNull("geo_point_2d")) {
//                                        val geoPoint = record.getJSONObject("geo_point_2d")
//                                        val lat = geoPoint.getDouble("lat")
//                                        val lon = geoPoint.getDouble("lon")
//                                        val address =
//                                            "Adresse : " + record.getString("adresse") + ", " + record.getString(
//                                                "arrondissement"
//                                            )
//                                        val pmrAccess =
//                                            "Accès PMR : " + record.getString("acces_pmr")
//                                        val type = record.getString("type")
//                                        val imageSrc: Int;
//                                        if (type == "SANISETTE" || type == "WC PUBLICS PERMANENTS") {
//                                            imageSrc = R.drawable.sanisette
//                                        } else if (type == "TOILETTES") {
//                                            imageSrc = R.drawable.toilette
//                                        } else {
//                                            imageSrc = R.drawable.urinoir
//                                        }
//                                        val openingHours =
//                                            "Horaires: " + record.getString("horaire")
//                                        val averageRating = 3.5f
//                                        val yourRating = 0f
//                                        val ficheURL = "google.com"
//
//                                        ToiletDataStore.toiletList.add(
//                                            Toilet(
//                                                id,
//                                                lat,
//                                                lon,
//                                                address,
//                                                type,
//                                                imageSrc,
//                                                openingHours,
//                                                pmrAccess,
//                                                averageRating,
//                                                yourRating,
//                                                ficheURL
//                                            )
//                                        )
//                                    } else {
//                                        println("Record $i doesn't have geo_point_2d")
//                                    }
//                                }
//
//                                withContext(Dispatchers.Main) {
//                                    responsesReceived++
//                                    val progress = (responsesReceived * 100) / totalRequests
//                                    loadingProgressBar.progress = progress
//                                    loadingProgressText.text = "Map loading : $progress%"
//
//                                    if (responsesReceived == totalRequests) {
//                                        activity?.let {
//                                            parentFragmentManager.beginTransaction()
//                                                .replace(R.id.fl_wrapper, MapFragment())
//                                                .addToBackStack(null)
//                                                .commit()
//                                        }
//                                    }
//                                }
//                            } else {
//                                withContext(Dispatchers.Main) {
//                                    Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT)
//                                        .show()
//                                }
//                            }
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(
//                                    context,
//                                    "Error while fetching data",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
//                        }
//                    }
//                }

}

