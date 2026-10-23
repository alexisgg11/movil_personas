package com.example.personas.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personas.viewmodel.PersonasViewModel

@Composable
fun PersonasApp(vm: PersonasViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val snack = remember { SnackbarHostState() }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    fun hasPermission() = Build.VERSION.SDK_INT < 37 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), vm::redPermitida)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.redPermitida(hasPermission()) }
    LaunchedEffect(Unit) {
        vm.redPermitida(hasPermission())
        if (Build.VERSION.SDK_INT >= 37 && !hasPermission() && !permissionRequested) {
            permissionRequested = true
            permission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }
    LaunchedEffect(s.mensaje) { s.mensaje?.let { snack.showSnackbar(it); vm.mensajeLeido() } }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), vm::camaraTerminada)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.importarFoto(it.toString()) } }
    val buscar = { focus.clearFocus(); vm.buscar() }
    Scaffold(snackbarHost = { SnackbarHost(snack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Personas", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    BotonPrimario(onClick = vm::nuevo, enabled = !s.ocupado) {
                        AppIcon(Symbol.Plus, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp)); Text("Nuevo")
                    }
                }
                if (!s.redPermitida) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Permite el acceso a la red local para buscar y guardar personas.", style = MaterialTheme.typography.bodyMedium)
                        BotonSecundario(onClick = {
                            if (permissionRequested) context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                            else if (Build.VERSION.SDK_INT >= 37) {
                                permissionRequested = true; permission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                            }
                        }) { Text("Permitir acceso") }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Etiqueta("Buscar por DNI")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CampoBusqueda(s.busqueda, vm::busqueda, !s.ocupado, buscar, Modifier.weight(1f))
                        BotonPrimario(onClick = buscar, enabled = !s.ocupado && s.redPermitida, modifier = Modifier.height(48.dp)) { Text("Buscar") }
                    }
                }
                if (s.ocupado && s.modo.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (s.sinResultado) Text("No se encontraron registros activos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.error != null && s.modo.isEmpty() && s.aviso == null) ErrorTexto(s.error!!)
                s.persona?.let { p -> ResultadoPersona(p, vm.fotoUrl(p.fotoUrl), !s.ocupado, vm::ver, vm::editar) { vm.confirmar("eliminar") } }
            }
        }
    }
    if (s.modo.isNotEmpty()) {
        FichaModal(s, vm, camera = {
            vm.prepararCamara()?.let { uri ->
                try { camera.launch(Uri.parse(uri)) }
                catch (_: Exception) { vm.camaraTerminada(false); vm.informar("No se encontró una aplicación de cámara disponible.") }
            }
        }, picker = {
            try { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            catch (_: Exception) { vm.informar("No se pudo abrir el selector de fotografías.") }
        })
    }
    DialogosPersona(s, vm)
}
