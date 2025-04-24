package com.example.toilets_finder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MarkerInfoBottomSheet(
    private val imageSrc: Int,
    private val type: String,
    private val address: String,
    //private val navigationUrl: String,
    private val averageRating: Float,
    private val yourRating: Float,
) : BottomSheetDialogFragment()  {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate sert à créer une View à partir du layout marker_info_bottom_sheet
        // Container (le parent de la View qu'on crée) est définit à false car la fenêtre d'informations est fermée au début (= la View n'est pas encore ajoutée au parent)
        val view = inflater.inflate(R.layout.marker_info_bottom_sheet, container, false)

        val image: ImageView = view.findViewById(R.id.bottom_sheet_image)
        val type: TextView = view.findViewById(R.id.bottom_sheet_type)
        val address: TextView = view.findViewById(R.id.bottom_sheet_address)
        val navigationButton: Button = view.findViewById(R.id.bottom_sheet_navigation)
        val averageRatingBar: RatingBar = view.findViewById(R.id.bottom_sheet_averagerating)
        val yourRatingBar: RatingBar = view.findViewById(R.id.bottom_sheet_yourrating)


        image.setImageResource(imageSrc)
        type.text = this.type
        address.text = this.address
        averageRatingBar.rating = averageRating
        yourRatingBar.rating = yourRating



        navigationButton.setOnClickListener {
            //
        }



        return view
    }
}