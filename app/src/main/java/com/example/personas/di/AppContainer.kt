package com.example.personas.di

import android.content.Context
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.personas.data.ArchivoFotosRepository
import com.example.personas.data.HttpPersonasRepository
import com.example.personas.domain.FotosRepository
import com.example.personas.domain.PersonasRepository
import com.example.personas.viewmodel.PersonasViewModel

class AppContainer(context: Context) {
    private val personas: PersonasRepository = HttpPersonasRepository()
    private val fotos: FotosRepository = ArchivoFotosRepository(context)

    val viewModelFactory = viewModelFactory {
        initializer { PersonasViewModel(personas, fotos, createSavedStateHandle()) }
    }
}
