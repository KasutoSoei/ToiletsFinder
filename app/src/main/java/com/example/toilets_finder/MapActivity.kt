package com.example.toilets_finder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL

class MapActivity : AppCompatActivity(), LocationListener {

    // late init because we init them later in the code
    private lateinit var map: MapView
    private lateinit var locationManager: LocationManager
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val MIN_TIME_UPDATE: Long = 1000
    private val MIN_DISTANCE_UPDATE: Float = 5f

    // Default coordinates (center of Paris, Notre-Dame)
    private val DEFAULT_LATITUDE = 48.8566
    private val DEFAULT_LONGITUDE = 2.3522

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val buttonBack: Button = findViewById(R.id.backButton)
        buttonBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        Configuration.getInstance().userAgentValue = packageName
        map = findViewById(R.id.map)
        // map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        getApiUrl()
    }

    private fun setupLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(this, "Aucun service de localisation disponible", Toast.LENGTH_SHORT).show()
                updateMapLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            } else {
                if (isGpsEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_UPDATE, MIN_DISTANCE_UPDATE, this)
                }
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_UPDATE, MIN_DISTANCE_UPDATE, this)
                }

                val lastKnownLocation: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                lastKnownLocation?.let {
                    updateMapLocation(it.latitude, it.longitude)
                } ?: updateMapLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        val userPoint = GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        map.controller.setZoom(15.0)
        map.controller.setCenter(userPoint)

        map.overlays.removeAll { it is Marker && it.title == "Votre position" }

        val userMarker = Marker(map)
        userMarker.position = userPoint
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userMarker.title = "Votre position"
        map.overlays.add(userMarker)
    }

    override fun onLocationChanged(location: Location) {
        updateMapLocation(location.latitude, location.longitude)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocation()
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show()
                updateMapLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDetach()
        locationManager.removeUpdates(this)
    }

    private fun getApiUrl() {
        for (i in 0 until 6) {
            val apiUrl = "https://opendata.paris.fr/api/explore/v2.1/catalog/datasets/sanisettesparis/records?limit=100&offset=" + 100 * i
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

                    withContext(Dispatchers.Main) {
                        for (i in 0 until records.length()) {
                            val record = records.getJSONObject(i)
                            val geoPoint = record.getJSONObject("geo_point_2d")
                            val lat = geoPoint.getDouble("lat")
                            val lon = geoPoint.getDouble("lon")
                            val address = record.getString("adresse")

                            addToiletMarker(lat, lon, address)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addToiletMarker(lat: Double, lon: Double, address: String) {
        val toiletMarker = Marker(map)
        toiletMarker.position = GeoPoint(lat, lon)
        toiletMarker.title = "Toilette : $address"
        map.overlays.add(toiletMarker)
    }
}
