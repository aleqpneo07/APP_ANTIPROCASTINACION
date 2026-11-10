package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OAuthSettingsState
import com.example.data.remote.GoogleCalendarService
import com.example.ui.NightPlannerUiState
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NightBorderSubtle
import com.example.ui.theme.NightCardElevated
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.NocturnalAmber
import com.example.ui.theme.SyncSuccessGreen
import com.example.ui.theme.TextMoonlight
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextStarlight
import kotlinx.coroutines.launch

@Composable
fun GoogleCalendarSetupScreen(
    state: NightPlannerUiState,
    onGeneratePkceAuthUrl: suspend () -> String,
    onSaveManualOAuthToken: (accessToken: String, refreshToken: String, email: String) -> Unit,
    onUpdateClientId: (String) -> Unit,
    onUpdateUnscheduledPolicy: (asAllDay: Boolean, startHour: Int, durationMin: Int) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val oAuth = state.oAuthSettings
    var clientIdInput by rememberSaveable(oAuth.clientId) { mutableStateOf(oAuth.clientId) }
    var accessTokenInput by rememberSaveable { mutableStateOf("") }
    var refreshTokenInput by rememberSaveable { mutableStateOf("") }
    var emailInput by rememberSaveable(oAuth.connectedEmail) {
        mutableStateOf(oAuth.connectedEmail.ifBlank { "aleqpneo.07@gmail.com" })
    }

    var unscheduledAsAllDay by rememberSaveable(oAuth.unscheduledAsAllDay) {
        mutableStateOf(oAuth.unscheduledAsAllDay)
    }
    var defaultStartHourStr by rememberSaveable(oAuth.defaultBlockStartHour) {
        mutableStateOf(oAuth.defaultBlockStartHour.toString())
    }
    var defaultDurationStr by rememberSaveable(oAuth.defaultBlockDurationMin) {
        mutableStateOf(oAuth.defaultBlockDurationMin.toString())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("google_calendar_setup_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. OAuth 2.0 Connection Status & Live Authorization Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardElevated),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (oAuth.isConnected) SyncSuccessGreen else ElectricIndigo
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (oAuth.isConnected) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (oAuth.isConnected) SyncSuccessGreen else ElectricIndigo,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Google Calendar API v3 · OAuth 2.0",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextMoonlight
                                )
                                Text(
                                    text = if (oAuth.isConnected) {
                                        "Conectado (${oAuth.connectedEmail.ifBlank { "Calendario primary" }})"
                                    } else {
                                        "Pendiente de vincular Access Token / Consentimiento OAuth"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (oAuth.isConnected) SyncSuccessGreen else NocturnalAmber
                                )
                            }
                        }

                        if (oAuth.isConnected) {
                            OutlinedButton(
                                onClick = onDisconnect,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("disconnect_oauth_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Desconectar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scope pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0A101E),
                        border = BorderStroke(1.dp, NightBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scope Oficial Configurado:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedSlate
                                )
                                Text(
                                    text = OAuthSettingsState.CALENDAR_EVENTS_SCOPE,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricIndigo
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(OAuthSettingsState.CALENDAR_EVENTS_SCOPE))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar scope",
                                    tint = TextStarlight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Provisioned OAuth Client ID field
                    OutlinedTextField(
                        value = clientIdInput,
                        onValueChange = { clientIdInput = it },
                        label = { Text("OAuth 2.0 Client ID (Google Cloud Console)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("oauth_client_id_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = { onUpdateClientId(clientIdInput) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Guardar Client ID",
                                    tint = SyncSuccessGreen
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option A: Launch Browser OAuth 2.0 PKCE Flow
                    Button(
                        onClick = {
                            scope.launch {
                                val url = onGeneratePkceAuthUrl()
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                runCatching { context.startActivity(intent) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("launch_browser_oauth_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricIndigo,
                            contentColor = Color(0xFF080C16)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Iniciar Flujo OAuth 2.0 con Google (PKCE)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = NightBorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option B: Direct OAuth 2.0 Access Token / Refresh Token Connection
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = NocturnalAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vinculación Directa con Token OAuth 2.0",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pega un Access Token válido con el scope calendar.events (ej. desde Google OAuth 2.0 Playground o tu cliente OAuth) para sincronizar en tiempo real con tu calendario primary.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Cuenta de Google vinculada") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = accessTokenInput,
                        onValueChange = { accessTokenInput = it },
                        label = { Text("Access Token OAuth 2.0 (ya29.a0...)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("oauth_access_token_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = refreshTokenInput,
                        onValueChange = { refreshTokenInput = it },
                        label = { Text("Refresh Token (Opcional para auto-renovación)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onSaveManualOAuthToken(accessTokenInput, refreshTokenInput, emailInput)
                            accessTokenInput = ""
                        },
                        enabled = accessTokenInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_oauth_token_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SyncSuccessGreen,
                            contentColor = Color(0xFF041A11)
                        )
                    ) {
                        Text(
                            text = "Vincular Token y Sincronizar Eventos de Mañana",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // 2. Policy Configuration for Tasks without Specific Time Block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                border = BorderStroke(1.dp, NightBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Comportamiento de Tareas sin Hora Específica",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Elige cómo se crean en Google Calendar las tareas que no tienen bloque horario definido:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = unscheduledAsAllDay,
                            onClick = {
                                unscheduledAsAllDay = true
                                onUpdateUnscheduledPolicy(
                                    true,
                                    defaultStartHourStr.toIntOrNull() ?: 9,
                                    defaultDurationStr.toIntOrNull() ?: 60
                                )
                            },
                            label = { Text("Evento 'Todo el día' (start.date)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !unscheduledAsAllDay,
                            onClick = {
                                unscheduledAsAllDay = false
                                onUpdateUnscheduledPolicy(
                                    false,
                                    defaultStartHourStr.toIntOrNull() ?: 9,
                                    defaultDurationStr.toIntOrNull() ?: 60
                                )
                            },
                            label = { Text("Bloque por defecto") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!unscheduledAsAllDay) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = defaultStartHourStr,
                                onValueChange = { defaultStartHourStr = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("Hora por defecto (0-23)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = defaultDurationStr,
                                onValueChange = { defaultDurationStr = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Duración (min)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    onUpdateUnscheduledPolicy(
                                        false,
                                        defaultStartHourStr.toIntOrNull() ?: 9,
                                        defaultDurationStr.toIntOrNull() ?: 60
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Aplicar")
                            }
                        }
                    }
                }
            }
        }

        // 3. Step-by-Step Google Cloud Console Guide Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                border = BorderStroke(1.dp, NocturnalAmber.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Guía Rápida Paso a Paso · Google Cloud Console",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NocturnalAmber
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Configuración exacta para desarrollo local (Web/PWA http://localhost:5173 y Android) con tu cuenta personal en modo prueba:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextStarlight
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GuideStepItem(
                        stepNumber = "1",
                        title = "Habilitar Google Calendar API v3",
                        body = "En console.cloud.google.com > APIs y servicios > Biblioteca, busca 'Google Calendar API' y pulsa Habilitar (Proyecto aprovisionado: powerful-asset-9fx4d)."
                    )
                    GuideStepItem(
                        stepNumber = "2",
                        title = "Pantalla de Consentimiento OAuth (Modo Prueba)",
                        body = "Ve a 'Pantalla de consentimiento de OAuth' > Tipo Externo > Estado de publicación: 'Pruebas' (Testing). En 'Usuarios de prueba' (Test users) pulsa '+ ADD USERS' y añade tu correo personal (ej. aleqpneo.07@gmail.com)."
                    )
                    GuideStepItem(
                        stepNumber = "3",
                        title = "Añadir el Scope de Calendar Events",
                        body = "En la sección 'Permisos' (Scopes), añade manualmente el permiso:\nhttps://www.googleapis.com/auth/calendar.events"
                    )
                    GuideStepItem(
                        stepNumber = "4",
                        title = "Crear Credenciales OAuth 2.0 (Client ID)",
                        body = "• Para Web / PWA (Vite): Crea 'ID de cliente de OAuth' > Tipo 'Aplicación web'. En 'Orígenes de JavaScript autorizados' añade exactamente:\n  http://localhost:5173\n• En 'URI de redireccionamiento autorizados' añade:\n  http://localhost:5173\n  ${GoogleCalendarService.REDIRECT_URI}"
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuideStepItem(
    stepNumber: String,
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0A101E),
        border = BorderStroke(1.dp, NightBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElectricIndigo.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Paso $stepNumber",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricIndigo,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextMoonlight
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedSlate
                )
            }
        }
    }
}
