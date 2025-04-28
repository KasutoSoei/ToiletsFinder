package com.example.toilets_finder

import io.github.jan.supabase.postgrest.from


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
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

class TempMapActivity : AppCompatActivity(), LocationListener {

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
        //setContentView(R.layout.activity_map)

        // Configuration of the map
        Configuration.getInstance().userAgentValue = packageName
        map = findViewById(R.id.map)
        // Uncomment/Comment the line below for hide/show the zoom buttons
        // map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

        // Check is the user location is allowed, else ask it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        //getApiUrl()
        fetchToiletsFromSupabase()

    }

    private fun setupLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            // Get the enabled status of the GPS and the Network
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            // Check if GPS or Network are enabled and get the position of the user, if both are not enabled display an error.
            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(this, "Aucun service de localisation disponible", Toast.LENGTH_SHORT).show()
                updateMapLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            } else {
                if (isGpsEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_UPDATE,
                        MIN_DISTANCE_UPDATE,
                        this
                    )
                    val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastKnownLocation != null) {
                        updateMapLocation(lastKnownLocation.latitude, lastKnownLocation.longitude)
                    }
                }
                else {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_UPDATE,
                        MIN_DISTANCE_UPDATE,
                        this
                    )
                    val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (lastKnownLocation != null) {
                        updateMapLocation(lastKnownLocation.latitude, lastKnownLocation.longitude)
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // Delete the last position of the user and add the new position
    private fun updateMapLocation(latitude: Double, longitude: Double) {
        // Actually the coordinate is in the center of Paris.
        // If you want the truth coordinate of your device, replace DEFAULT_LATITUDE and DEFAULT_LATITUDE bye latitude and longitude
        val userPoint = GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        map.controller.setZoom(18.0)
        map.controller.setCenter(userPoint)

        map.overlays.removeAll { it is Marker && it.title == "Votre position" }

        val userMarker = Marker(map)
        userMarker.position = userPoint
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userMarker.title = "Votre position"
        map.overlays.add(userMarker)
    }

    // Function from OSM (the map)
    // The function is called every MIN_TIME_UPDATE and MIN_DISTANCE_UPDATE
    override fun onLocationChanged(location: Location) {
        updateMapLocation(location.latitude, location.longitude)
    }

    // Function from OSM (the map)
    // The function is handling the response of the user when we ask him for the localisation permissions
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

    // When the map activity is stopped, stop to update the map and the localisation of the user
    override fun onDestroy() {
        super.onDestroy()
        map.onDetach()
        locationManager.removeUpdates(this)
    }

    // Create the 7 API Url we need.
    // The API can only give the data 100 by 100, for 623 we need 7 requests
    private fun getApiUrl() {
        for (i in 0 until 7) {
            val apiUrl = "https://opendata.paris.fr/api/explore/v2.1/catalog/datasets/sanisettesparis/records?where=geo_point_2d IS NOT NULL&limit=100&offset=${100 * i}"
            fetchToiletsFromSupabase()
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

                    val totalCount = jsonResponse.optInt("total_count", -1)
                    Log.d("API_DEBUG", "✅ Total available toilets in API: $totalCount from URL: $apiUrl")


                    val toilets = mutableListOf<ToiletInfo>()

                    withContext(Dispatchers.Main) {
                        for (i in 0 until records.length()) {
                            val record = records.getJSONObject(i)
                            val geoPoint = record.getJSONObject("geo_point_2d")

                            val lat = geoPoint.getDouble("lat")
                            val lon = geoPoint.getDouble("lon")
                            val address = record.getString("adresse")
                            val type = record.getString("type")
                            val horaire = record.getString("horaire")
                            val pmr = record.getString("acces_pmr") == "Oui"
                            val bebe = record.getString("relais_bebe") == "Oui"
                            val fiche = record.getString("url_fiche_equipement")


                            // Create ToiletInfo object
                            val toilet = ToiletInfo(
                                type = type,
                                address = address,
                                schedule = horaire,
                                pmrAccess = pmr,
                                babyRelay = bebe,
                                ficheUrl = fiche,
                                location = GeoPoint2D(lat = lat, lon = lon),
                            )

                            // Send to Supabase
                            toilets.add(toilet)
                            addToiletMarker(lat, lon, address)
                        }
                    }
                    // Insert toilets in Supabase (batched)
                    // sendToiletsBatchToSupabase(toilets)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                        val lat = toilet.location.lat
                        val lon = toilet.location.lon
                        val address = toilet.address
                        addToiletMarker(lat, lon, address)
                    }
                    Log.d("SUPABASE_FETCH", "Loaded ${response.size} toilets from Supabase.")
                }

            } catch (e: Exception) {
                Log.e("SUPABASE_FETCH", "Failed to fetch toilets: ${e.message}")
            }
        }
    }

    //show an overlay that allow us to save data in the database (supabase)
    private fun showToiletActionsDialog(toiletId: String) {

        val options = arrayOf("Ajouter aux favoris", "Blacklister", "Noter", "Commenter")
        val selected = BooleanArray(options.size)

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Toilette : $toiletId")

        builder.setMultiChoiceItems(options, selected) { _, index, isChecked ->
            selected[index] = isChecked
        }

        builder.setPositiveButton("") { _, _ ->




            Toast.makeText(this, "Action enregistrée !", Toast.LENGTH_SHORT).show()
        }


        builder.show()
    }

    // Add a marker on the map with the latitude and longitude in params.
    // Change the base icon of the marker for a custom icon ans resize it.
    private fun addToiletMarker(lat: Double, lon: Double, address: String) {
        val toiletMarker = Marker(map)
        toiletMarker.position = GeoPoint(lat, lon)
        toiletMarker.title = address

        val customIcon = ContextCompat.getDrawable(this, R.drawable.ic_toilet_marker) as BitmapDrawable
        val resizedIcon = Bitmap.createScaledBitmap(customIcon.bitmap, 35, 58, false)
        toiletMarker.icon = BitmapDrawable(resources, resizedIcon)
        toiletMarker.setOnMarkerClickListener { marker, _ ->
            //val toiletId = address

            //showToiletActionsDialog(toiletId)
            true
        }

        map.overlays.add(toiletMarker)

    }
}

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



