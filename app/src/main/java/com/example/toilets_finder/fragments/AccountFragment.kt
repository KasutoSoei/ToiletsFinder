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
import android.widget.TextView
import android.widget.Toast
import com.example.toilets_finder.MainActivity
import com.example.toilets_finder.Supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

class AccountFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        val btnLogin = view.findViewById<Button>(R.id.btn_login)
        val btnRegister = view.findViewById<Button>(R.id.btn_register)
        val btnLogout = view.findViewById<Button>(R.id.btn_logout)
        val textEmail = view.findViewById<TextView>(R.id.text_email)

        val userId = (requireActivity() as MainActivity).userId

        if (userId != null) {
            btnLogout.visibility = View.VISIBLE
            btnLogin.visibility = View.GONE
            btnRegister.visibility = View.GONE
            textEmail.visibility = View.GONE
        } else {
            btnLogout.visibility = View.GONE
        }

        btnLogin.setOnClickListener {
            showLoginDialog()
        }

        btnRegister.setOnClickListener {
            showRegisterDialog()
        }

        btnLogout.setOnClickListener {
            logoutUser()
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
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(requireContext(), "Connexion réussie", Toast.LENGTH_SHORT).show()

                        reloadApp()
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
                val id = UUID.randomUUID().toString()
                Supabase.client
                    .from("users")
                    .insert(mapOf(
                        "id" to id,
                        "email" to email,
                        "password" to password
                    ))

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Inscription réussie $email !", Toast.LENGTH_SHORT).show()

                    val sharedPref = requireContext().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("user_id", id)
                        apply()
                    }

                    reloadApp()
                }

            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Erreur d'inscription: ${e.message}", Toast.LENGTH_SHORT).show()
                    println("Erreur d'inscription: ${e.message}")
                }
            }
        }
    }

    private fun logoutUser() {
        val sharedPref = requireContext().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("user_id")
            apply()
        }
        Toast.makeText(requireContext(), "Déconnexion réussie", Toast.LENGTH_SHORT).show()

        reloadApp()
    }

    private fun reloadApp() {
        val intent = requireActivity().intent
        requireActivity().finish()
        startActivity(intent)
    }
}

@Serializable
data class User(
    val id: String,
    val name: String? = null,
    val email: String,
    val password: String
)
