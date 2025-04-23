package com.example.toilets_finder


import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LocalToiletRepository {
    private const val PREF_NAME = "toilet_actions"
    private const val KEY_ACTIONS = "actions"

    private lateinit var context: Context
    private val gson = Gson()
    private var cache: MutableMap<String, ToiletAction> = mutableMapOf()

    fun init(context: Context) {
        this.context = context.applicationContext
        loadActions()
    }

    private fun loadActions() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ACTIONS, null)
        if (json != null) {
            val type = object : TypeToken<MutableMap<String, ToiletAction>>() {}.type
            cache = gson.fromJson(json, type) ?: mutableMapOf()
        }
    }

    private fun saveActions() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_ACTIONS, gson.toJson(cache))
        editor.apply()
    }

    fun getAction(toiletId: String): ToiletAction? = cache[toiletId]

    fun saveAction(action: ToiletAction) {
        cache[action.toiletId] = action
        saveActions()
    }

    fun getAll(): List<ToiletAction> = cache.values.toList()
}