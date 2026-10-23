package com.example.personas

import android.app.Application
import com.example.personas.di.AppContainer

class PersonasApplication : Application() {
    val container by lazy { AppContainer(this) }
}
