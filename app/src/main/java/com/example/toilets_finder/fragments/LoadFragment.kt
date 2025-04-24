package com.example.toilets_finder.fragments

import android.os.Bundle
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
import com.example.toilets_finder.ToiletDataStore
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
    private lateinit var viewMapButton: Button
    private var totalRequests = 7
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
        fetchAllToiletsData()

        return view
    }


    private fun fetchAllToiletsData() {
        for (i in 0 until totalRequests) {
            val apiUrl =
                "https://opendata.paris.fr/api/explore/v2.1/catalog/datasets/sanisettesparis/records?limit=100&offset=" + 100 * i
            fetchToiletData(apiUrl)
        }
    }

    private fun fetchToiletData(apiUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    val records = jsonResponse.getJSONArray("results")

                    for (i in 0 until records.length()) {
                        val record = records.getJSONObject(i)
                        if (!record.isNull("geo_point_2d")) {
                            val geoPoint = record.getJSONObject("geo_point_2d")
                            val lat = geoPoint.getDouble("lat")
                            val lon = geoPoint.getDouble("lon")
                            val address = record.getString("adresse")

                            ToiletDataStore.toiletList.add(Triple(lat, lon, address))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        responsesReceived++
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
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error while fetching data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
