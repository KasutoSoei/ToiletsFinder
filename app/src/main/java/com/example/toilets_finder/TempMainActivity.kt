package com.example.toilets_finder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TempMainActivity : AppCompatActivity() {

    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var loadingProgressText: TextView
    private lateinit var viewMapButton: Button
    private var totalRequests = 7
    private var responsesReceived = 0
    private var toiletId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Clear the previous data from the data store to avoid duplicates
        ToiletDataStore.toiletList.clear()

        // Initialize the UI elements
        loadingProgressBar = findViewById<ProgressBar>(R.id.loadingProgressBar)
        loadingProgressText = findViewById<TextView>(R.id.loadingProgressTextView)
        viewMapButton = findViewById<Button>(R.id.mapButton)

        // Set the viewMapButton actions
        viewMapButton.setOnClickListener(View.OnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            loadingProgressText.visibility = View.VISIBLE
            loadingProgressBar.progress = 0
            loadingProgressText.text = "Loading map : 0%"
            viewMapButton.visibility = View.GONE
            fetchAllToiletsData()
        })

    }

    // Create the 7 API URLs we need.
    // (The API can only provide data 100 results by 100, so for 623 results we need 7 requests)
    private fun fetchAllToiletsData() {
        for (i in 0 until totalRequests) {
            val apiUrl =
                "https://opendata.paris.fr/api/explore/v2.1/catalog/datasets/sanisettesparis/records?limit=100&offset=" + 100 * i
            print(apiUrl)
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
                            val id = toiletId++
                            println("Toilette n° : " + id)
                            val geoPoint = record.getJSONObject("geo_point_2d")
                            val lat = geoPoint.getDouble("lat")
                            val lon = geoPoint.getDouble("lon")
                            val address = "Adresse : " + record.getString("adresse") + ", " + record.getString("arrondissement")
                            val pmrAccess = "Accès PMR : " + record.getString("acces_pmr")
                            val type = record.getString("type")
                            val imageSrc: Int;
                            if (type == "SANISETTE" || type == "WC PUBLICS PERMANENTS") {
                                imageSrc = R.drawable.sanisette
                            }
                            else if (type == "TOILETTES"){
                                imageSrc = R.drawable.toilette
                            }
                            else {
                                imageSrc = R.drawable.urinoir
                            }
                            val openingHours = "Horaires: " + record.getString("horaire")
                            val averageRating = 3.5f
                            val yourRating = 0f

                            ToiletDataStore.toiletList.add(Toilet(id, lat, lon, address, type, imageSrc, openingHours, pmrAccess, averageRating, yourRating))
                        }
                        else {
                            println("Record $i doesn't have geo_point_2d")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        responsesReceived++
                        val progress = (responsesReceived * 100) / totalRequests
                        println("Progress: $progress")
                        loadingProgressBar.progress = progress
                        loadingProgressText.text = "Map loading : $progress%"

                        if (responsesReceived == totalRequests) {
                            println("All data received, launching MapActivity")
                            if (!isFinishing && !isDestroyed) {
                                val intent = Intent(this@MainActivity, MapActivity::class.java)
                                startActivity(intent)
                            }
                        }
                    }
                }
                else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Connection error", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error while fetching data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}