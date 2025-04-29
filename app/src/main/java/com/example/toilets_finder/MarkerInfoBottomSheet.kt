package com.example.toilets_finder

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.serialization.Serializable

class MarkerInfoBottomSheet(
    private val toiletId: String,
    private val imageSrc: Int,
    private val type: String,
    private val address: String,
    private val openingHours: String,
    private val pmrAccess: String,
    private var averageRating: Float,
    private var yourRating: Float,
    private val ficheURL: String,
) : BottomSheetDialogFragment()  {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Supabase.init()

        // Inflate sert à créer une View à partir d'un layout, en l'occurence marker_info_bottom_sheet
        // container (le parent de la View qu'on crée) est définit à false car la fenêtre d'informations est fermée au début ( = la View n'est pas encore ajoutée au parent)
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

        if (ficheURL != "null") {
            println("fiche URL" + ficheURL)
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
        }
        else if (openingHours.text == "Horaires: null"){
            openingHours.text = "Horaires: Pas de données"
        }
        else {
            openingHours.text = this.openingHours
        }

        yourRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
            val userId = (requireActivity() as MainActivity).userId
            if (userId != null) {
                yourRating = rating
                yourRatingBar.rating = rating
                println("Note changée à: $rating")
                saveRatingToDatabase(toiletId, userId, yourRating)
            }
            else {
                yourRatingBar.rating = 0f
                Toast.makeText(requireContext(), "Vous devez être connecter pour noter", Toast.LENGTH_LONG).show()
            }

        }
        return view
    }

    private fun saveRatingToDatabase(toiletId: String, userId: String, rating: Float) {
        CoroutineScope(Dispatchers.IO).launch {
            val id = UUID.randomUUID().toString()
            val entry = RatingEntry(
                id = id,
                toilet_id = toiletId,
                user_id = userId,
                rating = rating
            )
            Supabase.client
                .from("reviews")
                .insert(entry)
        }
    }
}

@Serializable
data class RatingEntry(
    val id: String,
    val toilet_id: String,
    val user_id: String,
    val rating: Float
)


