package com.example.toilets_finder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity(), LocationListener {

    private lateinit var map: MapView
    private lateinit var locationManager: LocationManager
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val MIN_TIME_UPDATE: Long = 1000 // 1 seconde entre chaque maj
    private val MIN_DISTANCE_UPDATE: Float = 5f // Maj tous les 5 mètres

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Configuration d'OSMDroid
        Configuration.getInstance().userAgentValue = packageName

        // Initialisation de la carte
        map = findViewById(R.id.map)

        map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        map.isClickable = false
        map.setMultiTouchControls(false)

        // Vérification des permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setupLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setupLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            // On privilégie le GPS pour plus de précision
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(this, "Aucun service de localisation disponible", Toast.LENGTH_SHORT).show()
            } else {
                // Si GPS dispo, on l’utilise en priorité
                if (isGpsEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_UPDATE,
                        MIN_DISTANCE_UPDATE,
                        this
                    )
                }

                // Si réseau dispo, on le prend en backup
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_UPDATE,
                        MIN_DISTANCE_UPDATE,
                        this
                    )
                }

                // Dernière position connue (fallback rapide)
                val lastKnownLocation: Location? = locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                ) ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                lastKnownLocation?.let {
                    updateMapLocation(it.latitude, it.longitude)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        val userPoint = GeoPoint(latitude, longitude)

        // Centrer la carte sur la position de l'utilisateur
        map.controller.setZoom(15.0)
        map.controller.setCenter(userPoint)

        // Ajouter un marqueur pour la position actuelle
        val userMarker = Marker(map)
        userMarker.position = userPoint
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userMarker.title = "Vous êtes ici"
        map.overlays.clear()
        map.overlays.add(userMarker)

        Toast.makeText(this, "Position mise à jour : $latitude, $longitude", Toast.LENGTH_SHORT).show()
    }

    override fun onLocationChanged(location: Location) {
        updateMapLocation(location.latitude, location.longitude)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocation()
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDetach()
        locationManager.removeUpdates(this)
    }
}
