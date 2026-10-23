package com.example.personas.di

import android.content.Context
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.personas.data.ApiClient
import com.example.personas.data.HttpUbigeoRepository
import com.example.personas.domain.UbigeoRepository
import com.example.personas.data.ArchivoFotosRepository
import com.example.personas.data.HttpPersonasRepository
import com.example.personas.domain.FotosRepository
import com.example.personas.domain.PersonasRepository
import com.example.personas.viewmodel.FormularioSavedState
import com.example.personas.viewmodel.PersonasViewModel

class AppContainer(context: Context) {
    private val api = ApiClient()
    private val personas: PersonasRepository = HttpPersonasRepository(api)
    private val ubigeo: UbigeoRepository = HttpUbigeoRepository(api)
    private val fotos: FotosRepository = ArchivoFotosRepository(context)

    val viewModelFactory = viewModelFactory {
        initializer { PersonasViewModel(personas, fotos, FormularioSavedState(createSavedStateHandle()), ubigeo) }
    }
}
