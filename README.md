# 🌙 NightPlanner — App Anti-Procrastinación

**NightPlanner** es una aplicación nativa para Android desarrollada en **Kotlin y Jetpack Compose** diseñada bajo la psicología del método *Ivy Lee* y *Time Blocking* nocturno, con sincronización bidireccional en tiempo real con **Google Calendar API v3**.

> *"Planifica esta noche. Ejecuta mañana sin fricción."*

---

## ✨ Características Principales

1. **🌙 Modo Planificación Nocturna (`Planificar Mañana`)**
   - Descarga cognitiva antes de dormir: organiza de 3 a 5 bloques clave para el día siguiente.
   - **QuickInputParser (Entrada Rápida Inteligente):** Soporta etiquetas en línea como `#uni`, `#gym`, `#proyectos`, `#personal`, `#salud`, prioridades `!alta`, `!media`, `!baja`, rangos horarios `@09:00-11:00` y duraciones `+45m`.
   - **Lectura en vivo de Google Calendar:** Importa los eventos existentes de mañana para evitar solapamientos.
   - **Confirmación en Lote:** Sincroniza todas las tareas planificadas hacia tu calendario principal (`primary`) con un solo toque.

2. **☀️ Modo Ejecución Diaria (`Hoy`)**
   - Interfaz libre de distracciones centrada en completar los bloques planificados la noche anterior.
   - Barra de progreso animada y filtrado por categorías.
   - **Sincronización de Completado (`events.patch`):** Al marcar una tarea como completada, el evento en Google Calendar se actualiza automáticamente con el prefijo `✅ ` y color verde albahaca (`colorId = 10`).
   - **Rollover 1-Clic (`Migrar pendientes a mañana`):** Reprograma las tareas incompletas de hoy al plan de mañana y actualiza sus fechas en Google Calendar.

3. **📅 Agenda 24h & Prevención de Solapamientos**
   - Línea de tiempo cronológica unificada (eventos de Google Calendar + bloques de NightPlanner).
   - Detector automático de colisiones horarias y cálculo de **Huecos Libres** entre las 08:00 y las 22:00 con asignación rápida en 1 toque.

4. **🔐 Integración Google Calendar API v3 (OAuth 2.0 PKCE + Token Directo)**
   - Flujo completo **OAuth 2.0 con PKCE (S256)** mediante Deep Link (`com.aistudio.nightplanner.vxkpmq:/oauth2redirect`).
   - Vinculación directa con Access Token / Refresh Token para pruebas inmediatas.

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin 2.2.10
- **UI:** Jetpack Compose (Material 3, diseño *Nocturnal Luxury Minimalist*, adaptativo móvil/tablet con `NavigationBar` y `NavigationRail`)
- **Arquitectura:** MVVM (`NightPlannerViewModel` + `NightPlannerRepository` + `StateFlow`)
- **Base de Datos Local:** Room SQLite (`NightPlannerDatabase`, `NightTaskDao`)
- **Preferencias:** Jetpack DataStore Preferences (`OAuthPreferencesRepository`)
- **Red & Serialización:** Retrofit 2.12 + OkHttp 4.10 + Moshi 1.15

---

## 🚀 Compilación e Instalación del APK

Para generar el APK instalable desde la terminal:

```bash
./gradlew assembleDebug
```

El archivo APK firmado se generará en:
`app/build/outputs/apk/debug/app-debug.apk`
