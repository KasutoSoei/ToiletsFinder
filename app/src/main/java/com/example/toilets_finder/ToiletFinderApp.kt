package com.example.toilets_finder

import android.app.Application

class ToiletFinderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Supabase.init()
    }
}
