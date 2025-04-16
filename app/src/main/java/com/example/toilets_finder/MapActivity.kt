package com.example.toilets_finder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.graphics.scale
import androidx.core.graphics.drawable.toDrawable

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


        // get the "Back" button, to go back to the home page
        val buttonBack: Button = findViewById(R.id.backButton)
        buttonBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Configuration of the map
        Configuration.getInstance().userAgentValue = packageName
        map = findViewById(R.id.map)
        // Uncomment/Comment the line below for hide/show the zoom buttons
        // map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

        // Check if the user location is allowed, else ask it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        addAllToiletsMarkers()
    }

    private fun setupLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            // Get the enabled status of the GPS and the Network
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            // Check if GPS or Network are enabled and get the position of the user, if both are not enabled display an error.
            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(this, "No location service available", Toast.LENGTH_SHORT).show()
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

    // Update the map with the new user's position, based on his coordinates
    private fun updateMapLocation(latitude: Double, longitude: Double) {
        // By default the coordinates are the center of Paris
        // If you want the true coordinates of your device, replace DEFAULT_LATITUDE and DEFAULT_LONGITUDE by latitude and longitude
        val userPoint = GeoPoint(latitude, longitude)
        map.controller.setZoom(15.0)
        map.controller.setCenter(userPoint)

        // Remove the last user position marker
        map.overlays.removeAll { it is Marker && it.title == "You are here" }

        val userMarker = Marker(map)
        userMarker.position = userPoint
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userMarker.title = "You are here"
        map.overlays.add(userMarker)
    }

    // Function from OSM (the map)
    // This function is called every MIN_TIME_UPDATE and MIN_DISTANCE_UPDATE
    override fun onLocationChanged(location: Location) {
        updateMapLocation(location.latitude, location.longitude)
    }

    // Function from OSM (the map)
    // This function is handling the response of the user when we ask him for the localisation permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocation()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
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


    // Adds on the map markers for all the toilets, from the data stored in ToiletDataStore
    private fun addAllToiletsMarkers() {
        for (toilet in ToiletDataStore.toiletList) {
            val lat = toilet.first
            val lon = toilet.second
            val address = toilet.third
            addToiletMarker(lat, lon, address)
        }
    }


    // Adds a marker on the map with the latitude and longitude in params.
    // Change the base icon of the marker with a custom icon and resize it.
    private fun addToiletMarker(lat: Double, lon: Double, address: String) {
        val toiletMarker = Marker(map)
        toiletMarker.position = GeoPoint(lat, lon)
        toiletMarker.title = address

        val customIcon = ContextCompat.getDrawable(this, R.drawable.ic_toilet_marker) as BitmapDrawable
        val resizedIcon = customIcon.bitmap.scale(35, 58, false)
        toiletMarker.icon = resizedIcon.toDrawable(resources)

        map.overlays.add(toiletMarker)
    }
}
