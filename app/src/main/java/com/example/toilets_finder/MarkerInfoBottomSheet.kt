package com.example.toilets_finder

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.R.bool
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
    private val openingHours: String,
    private val pmrAccess: String,
    //private val navigationUrl: String,
    private val averageRating: Float,
    private val yourRating: Float,
    private val ficheURL: String,
) : BottomSheetDialogFragment()  {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate sert à créer une View à partir du layout marker_info_bottom_sheet
        // Container (le parent de la View qu'on crée) est définit à false car la fenêtre d'informations du marker est fermée au début ( = la View n'est pas encore ajoutée au parent)
        val view = inflater.inflate(R.layout.marker_info_bottom_sheet, container, false)

        val image: ImageView = view.findViewById(R.id.bottom_sheet_image)
        val type: TextView = view.findViewById(R.id.bottom_sheet_type)
        val address: TextView = view.findViewById(R.id.bottom_sheet_address)
        val openingHours: TextView = view.findViewById(R.id.bottom_sheet_openinghours)
        val pmrAccess: TextView = view.findViewById(R.id.bottom_sheet_pmraccess)
        val averageRatingBar: RatingBar = view.findViewById(R.id.bottom_sheet_averagerating)
        val yourRatingBar: RatingBar = view.findViewById(R.id.bottom_sheet_yourrating)


        image.setImageResource(imageSrc)
        type.text = this.type
        address.text = this.address
        openingHours.text = this.openingHours
        pmrAccess.text = this.pmrAccess
        averageRatingBar.rating = averageRating
        yourRatingBar.rating = yourRating
        println(ficheURL)

        if (ficheURL != "null") {
            println(ficheURL)
            val spannableString = SpannableString("Voir fiche équipement")
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ficheURL))
                    startActivity(intent)
                }
            }
            spannableString.setSpan(clickableSpan, 0, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            openingHours.text = spannableString
            openingHours.movementMethod = LinkMovementMethod.getInstance()
        } else {
            openingHours.text = this.openingHours
        }

        return view
    }
}