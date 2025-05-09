package com.example.toilets_finder.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import com.example.toilets_finder.MarkerInfoBottomSheet
import com.example.toilets_finder.R
import com.example.toilets_finder.Toilet
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.text.Editable
import android.text.TextWatcher


private lateinit var searchEditText: EditText
private lateinit var filterButton: ImageButton
private var filterByType: String? = null
private var filterByPmr: Boolean? = null


class MapFragment : Fragment(), LocationListener {

    // late init because we initialize them later in the code
    private lateinit var map: MapView
    private lateinit var locationManager: LocationManager

    // Default coordinates (center of Paris, Notre-Dame)
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val MIN_TIME_UPDATE: Long = 1000
    private val MIN_DISTANCE_UPDATE: Float = 5f

    private val DEFAULT_LATITUDE = 48.8566
    private val DEFAULT_LONGITUDE = 2.3522

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Map configuration
        Configuration.getInstance().userAgentValue = requireContext().packageName
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Map initialization
        map = view.findViewById(R.id.map)
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.setMultiTouchControls(true)
        map.invalidate()

        // Check location permission, if not granted, request it
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            setupLocation()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        //verify every search bar input
        searchEditText = view.findViewById(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                filterToilets(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        //show the dialog when the filter button is clicked
        filterButton = view.findViewById(R.id.filterButton)
        filterButton.setOnClickListener {
            showFilterDialog()
        }



        // Add all toilet markers to the map
        addAllToiletsMarkers()
        return view
    }

    private fun setupLocation() {
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            // Check if GPS or Network are enabled
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            // If neither is enabled, show an error message
            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(context, "No location service available", Toast.LENGTH_SHORT).show()
                updateMapLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            } else {
                val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
                locationManager.requestLocationUpdates(provider, MIN_TIME_UPDATE, MIN_DISTANCE_UPDATE, this)
                val lastKnownLocation = locationManager.getLastKnownLocation(provider)
                lastKnownLocation?.let {
                    updateMapLocation(it.latitude, it.longitude)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        // Replace lat and long by DEFAULT_LATITUDE and DEFAULT_LONGITUDE if you are on a emulator
        val userPoint = GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        map.controller.setZoom(18.0)
        map.controller.setCenter(userPoint)

        // Remove the previous "You are here" marker
        map.overlays.removeAll { it is Marker && it.title == "Vous êtes ici" }

        // Add a new marker for the user's current location
        val userMarker = Marker(map)
        userMarker.position = userPoint
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userMarker.title = "Vous êtes ici"
        map.overlays.add(userMarker)
    }

    // Called every time the location is updated
    override fun onLocationChanged(location: Location) {
        updateMapLocation(location.latitude, location.longitude)
    }

    // Handle the user's response to the location permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupLocation()
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            updateMapLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        }
    }

    // Stop updating the user's location when the fragment is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        map.onDetach()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
    }

    // Add markers for all toilets from ToiletDataStore
    private fun addAllToiletsMarkers() {
        for (toilet in com.example.toilets_finder.ToiletDataStore.toiletList) {
            val id = toilet.id
            val lat = toilet.latitude
            val lon = toilet.longitude
            val address = toilet.address
            addToiletMarker(toilet)
        }
    }

    // dynamically search and filter
    private fun updateMarkers(filteredList: List<Toilet>) {
        map.overlays.clear()
        for (toilet in filteredList) {
            addToiletMarker(toilet)
        }
    }

    // Add a toilet marker to the map with custom resized icon
    private fun addToiletMarker(toilet: Toilet) {
        val toiletMarker = Marker(map)
        toiletMarker.position = GeoPoint(toilet.latitude, toilet.longitude)

        val customIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_toilet_marker) as BitmapDrawable
        val resizedIcon = customIcon.bitmap.scale(35, 58, false)
        toiletMarker.icon = resizedIcon.toDrawable(resources)

        // Bind the Toilet object to the marker so we can retrieve it later
        toiletMarker.relatedObject = toilet

        // Retrieve the Toilet object we attached to the marker, when the latter is clicked
        // Then create the bottom sheet with the toilet informations
        toiletMarker.setOnMarkerClickListener { clickedMarker, _ ->
            val clickedToilet = clickedMarker.relatedObject as Toilet

            val bottomSheet = MarkerInfoBottomSheet(
                toiletId = clickedToilet.id,
                imageSrc = clickedToilet.imageSrc,
                type = clickedToilet.type,
                address = clickedToilet.address,
                openingHours = clickedToilet.openingHours,
                pmrAccess = clickedToilet.pmrAccess,
                averageRating = clickedToilet.averageRating,
                yourRating = clickedToilet.yourRating,
                ficheURL = clickedToilet.ficheUrl)
            bottomSheet.show(parentFragmentManager, "marker_info")
            true
        }

        map.overlays.add(toiletMarker)
    }

    //chack the the corresponding filter in the data base
    private fun filterToilets(searchQuery: String = "") {
        val filteredList = com.example.toilets_finder.ToiletDataStore.toiletList.filter { toilet ->
            val matchesAddress = toilet.address.contains(searchQuery, ignoreCase = true)
            val matchesType = filterByType?.let { toilet.type.equals(it, ignoreCase = true) } ?: true
            val matchesPmr = filterByPmr?.let { toilet.pmrAccess.contains("Oui", ignoreCase = true) == it } ?: true
            matchesAddress && matchesType && matchesPmr
        }
        updateMarkers(filteredList)
    }

    //dialog withe the option
    private fun showFilterDialog() {
        val types = arrayOf(
            "LAVATORY", "SANISETTE", "TOILETTES", "URINOIR", "URINOIR FEMME", "WC PUBLICS PERMANENTS"
        )
        val selectedType = BooleanArray(types.size)

        val pmrOptions = arrayOf("Accès PMR uniquement")
        val selectedPmr = BooleanArray(pmrOptions.size)

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Filtres")

        builder.setMultiChoiceItems(types + pmrOptions, BooleanArray(types.size + 1)) { dialog, index, isChecked ->
            if (index < types.size) {
                filterByType = if (isChecked) types[index] else null
            } else {
                filterByPmr = if (isChecked) true else null
            }
        }

        builder.setPositiveButton("Appliquer") { _, _ ->
            filterToilets(searchEditText.text.toString())
        }
        builder.setNegativeButton("Réinitialiser") { _, _ ->
            filterByType = null
            filterByPmr = null
            filterToilets("")
        }
        builder.show()
    }



}
