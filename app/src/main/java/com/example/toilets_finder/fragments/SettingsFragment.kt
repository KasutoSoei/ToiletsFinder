package com.example.toilets_finder.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.toilets_finder.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.EditText
import android.widget.Toast
import com.example.toilets_finder.Supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.util.UUID

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val btnLogin = view.findViewById<Button>(R.id.btn_login)
        val btnRegister = view.findViewById<Button>(R.id.btn_register)

        btnLogin.setOnClickListener {
            showLoginDialog()
        }

        btnRegister.setOnClickListener {
            showRegisterDialog()
        }

        return view
    }

    private fun showLoginDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_login, null)
        dialog.setContentView(view)

        val emailEditText = view.findViewById<EditText>(R.id.et_email_login)
        val passwordEditText = view.findViewById<EditText>(R.id.et_password_login)
        val loginButton = view.findViewById<Button>(R.id.btn_login_confirm)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
                Toast.makeText(requireContext(), "Connexion de $email", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showRegisterDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_register, null)
        dialog.setContentView(view)

        val emailEditText = view.findViewById<EditText>(R.id.et_email_register)
        val passwordEditText = view.findViewById<EditText>(R.id.et_password_register)
        val confirmPasswordEditText = view.findViewById<EditText>(R.id.et_password_confirm_register)
        val registerButton = view.findViewById<Button>(R.id.btn_register_confirm)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    registerUser(email, password)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun loginUser(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = Supabase.client
                    .from("users")
                    .select {
                        filter {
                            eq("email", email)
                        }
                    }
                    .decodeSingle<User>()


                if (response.password == password) {
                    val sharedPref = requireContext().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("user_id", response.id)
                        apply()
                        val sharedPref = requireContext().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
                        val userId = sharedPref.getString("user_id", null)
                        println(userId)
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        println("Bienvenue ${response.email} !")
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        println("Mot de passe incorrect")
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    println("Utilisateur non trouvé ou erreur: ${e.message}")
                }
            }
        }
    }


    private fun registerUser(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client
                    .from("users")
                    .insert(mapOf(
                        "id" to UUID.randomUUID().toString(),
                        "email" to email,
                        "password" to password
                    ))

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Inscription réussie pour $email !", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Erreur d'inscription: ${e.message}", Toast.LENGTH_SHORT).show()
                    println("Erreur d'inscription: ${e.message}")
                }
            }
        }
    }
}

@Serializable
data class User(
    val id: String,
    val name: String? = null,
    val email: String,
    val password: String
)
