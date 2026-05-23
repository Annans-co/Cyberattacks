package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AttackEvent
import com.example.data.model.IncidentEntity
import com.example.data.repository.ThreatRepository
import com.example.data.api.GeminiThreatAnalyst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class ThreatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ThreatRepository
    
    // UI reactive data stream from Room db
    val loggedIncidents: StateFlow<List<IncidentEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ThreatRepository(database.incidentDao())
        loggedIncidents = repository.allIncidents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Live Simulated Cyber Attacks
    private val _activeAttacks = MutableStateFlow<List<AttackEvent>>(emptyList())
    val activeAttacks: StateFlow<List<AttackEvent>> = _activeAttacks.asStateFlow()

    // Dashboard Statistics Metrics
    private val _totalAttacksToday = MutableStateFlow(128450)
    val totalAttacksToday: StateFlow<Int> = _totalAttacksToday.asStateFlow()

    private val _threatLevelStats = MutableStateFlow(mapOf("LOW" to 20, "MEDIUM" to 42, "HIGH" to 28, "CRITICAL" to 10))
    val threatLevelStats: StateFlow<Map<String, Int>> = _threatLevelStats.asStateFlow()

    private val _mostTargetedPort = MutableStateFlow(443)
    val mostTargetedPort: StateFlow<Int> = _mostTargetedPort.asStateFlow()

    // Selected Incident details (for Inspector panel)
    private val _selectedIncident = MutableStateFlow<IncidentEntity?>(null)
    val selectedIncident: StateFlow<IncidentEntity?> = _selectedIncident.asStateFlow()

    // AI Analysis State: "IDLE", "LOADING", "SUCCESS", "ERROR"
    private val _aiState = MutableStateFlow<String>("IDLE")
    val aiState: StateFlow<String> = _aiState.asStateFlow()

    private val _aiReply = MutableStateFlow<String>("")
    val aiReply: StateFlow<String> = _aiReply.asStateFlow()

    // Geolocation database for beautiful Canvas Mercator positioning
    private data class GeoNode(val country: String, val code: String, val lat: Float, val lon: Float)

    private val nodesList = listOf(
        GeoNode("United States", "US", 0.22f, 0.40f),
        GeoNode("Canada", "CA", 0.24f, 0.30f),
        GeoNode("Germany", "DE", 0.50f, 0.35f),
        GeoNode("United Kingdom", "UK", 0.47f, 0.34f),
        GeoNode("Netherlands", "NL", 0.49f, 0.34f),
        GeoNode("Russia", "RU", 0.65f, 0.30f),
        GeoNode("China", "CN", 0.75f, 0.45f),
        GeoNode("Brazil", "BR", 0.35f, 0.70f),
        GeoNode("South Korea", "KR", 0.79f, 0.43f),
        GeoNode("India", "IN", 0.68f, 0.52f),
        GeoNode("Japan", "JP", 0.83f, 0.42f),
        GeoNode("Iran", "IR", 0.58f, 0.46f),
        GeoNode("Israel", "IL", 0.55f, 0.45f),
        GeoNode("Singapore", "SG", 0.74f, 0.61f),
        GeoNode("Australia", "AU", 0.83f, 0.75f),
        GeoNode("Switzerland", "CH", 0.49f, 0.37f),
        GeoNode("France", "FR", 0.48f, 0.36f),
        GeoNode("Saudi Arabia", "SA", 0.56f, 0.50f)
    )

    private val attackVectorsList = listOf(
        Pair("DDoS Flood Campaign", "CRITICAL"),
        Pair("SQL Injection Attempt", "HIGH"),
        Pair("SSH Bruteforce Ingress", "MEDIUM"),
        Pair("Phishing Leak Exploit", "HIGH"),
        Pair("CVE-2023-44487 Rapid Reset", "CRITICAL"),
        Pair("Zero-Day Buffer Overflow", "CRITICAL"),
        Pair("Ransomware Payload Drop", "CRITICAL"),
        Pair("Cross-Site Scripting (XSS)", "LOW"),
        Pair("API Directory Traversal", "MEDIUM"),
        Pair("Malicious DLL Injection", "HIGH"),
        Pair("DNS Cache Poisoning", "MEDIUM"),
        Pair("Remote Code Execution (RCE)", "CRITICAL")
    )

    private val portsList = listOf(80, 443, 22, 21, 3389, 445, 8080, 23, 1433, 3306)

    init {
        // Run Real-time Attack Simulation Ticker Loop
        viewModelScope.launch {
            // Seed initial list with 6 events
            repeat(6) {
                val initEvent = generateSampleAttack()
                _activeAttacks.value = _activeAttacks.value + initEvent
            }

            while (true) {
                delay(Random.nextLong(1500, 3000)) // Simulation tick
                val newEvent = generateSampleAttack()
                
                // Add to list, keeping max size bounded at 25 for performance
                val current = _activeAttacks.value.toMutableList()
                current.add(0, newEvent)
                if (current.size > 25) {
                    current.removeAt(current.size - 1)
                }
                _activeAttacks.value = current

                // Update Metrics Counters
                _totalAttacksToday.value += Random.nextInt(1, 4)
                
                // Recalculate quick stats randomly
                val updatedStats = _threatLevelStats.value.toMutableMap()
                val eventLevel = newEvent.threatLevel
                updatedStats[eventLevel] = (updatedStats[eventLevel] ?: 0) + 1
                _threatLevelStats.value = updatedStats

                if (Random.nextFloat() > 0.82f) {
                    _mostTargetedPort.value = portsList.random()
                }
            }
        }
    }

    private fun generateSampleAttack(): AttackEvent {
        val id = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val attacker = nodesList.random()
        var target = nodesList.random()
        while (target.code == attacker.code) {
            target = nodesList.random() // Ensure attacker separate from victim
        }
        val (vector, level) = attackVectorsList.random()
        val port = portsList.random()
        val severity = when (level) {
            "LOW" -> Random.nextInt(5, 30)
            "MEDIUM" -> Random.nextInt(31, 60)
            "HIGH" -> Random.nextInt(61, 85)
            "CRITICAL" -> Random.nextInt(86, 100)
            else -> 50
        }
        val payload = when (level) {
            "LOW" -> "${Random.nextInt(1, 100)} KB"
            "MEDIUM" -> "${Random.nextInt(101, 999)} KB"
            "HIGH" -> "${String.format("%.1f", Random.nextDouble(1.0, 50.0))} MB"
            "CRITICAL" -> "${String.format("%.1f", Random.nextDouble(1.0, 10.0))} Gbps"
            else -> "512 KB"
        }

        return AttackEvent(
            id = id,
            timestamp = timestamp,
            attackerIp = generateRandomIp(),
            attackerCountry = attacker.country,
            attackerCountryCode = attacker.code,
            attackerLat = attacker.lat,
            attackerLon = attacker.lon,
            targetIp = generateRandomIp(),
            targetCountry = target.country,
            targetCountryCode = target.code,
            targetLat = target.lat,
            targetLon = target.lon,
            attackType = vector,
            threatLevel = level,
            port = port,
            severityScore = severity,
            payloadSize = payload
        )
    }

    private fun generateRandomIp(): String {
        return "${Random.nextInt(1, 255)}.${Random.nextInt(0, 255)}.${Random.nextInt(0, 255)}.${Random.nextInt(1, 255)}"
    }

    // --- Threat Logging Operations ---

    fun fileIncidentReport(event: AttackEvent) {
        viewModelScope.launch {
            val incident = IncidentEntity(
                timestamp = event.timestamp,
                attackerIp = event.attackerIp,
                attackerCountry = event.attackerCountry,
                attackerCountryCode = event.attackerCountryCode,
                targetIp = event.targetIp,
                targetCountry = event.targetCountry,
                targetCountryCode = event.targetCountryCode,
                attackType = event.attackType,
                threatLevel = event.threatLevel,
                port = event.port,
                payloadSize = event.payloadSize,
                status = "INVESTIGATING",
                analystNotes = "Active tracking initiated. Port quarantine check needed.",
                playbookName = "Port Sealing & Core Routing Deflect",
                aiBriefing = "" // Populated on-demand by Gemini
            )
            repository.logIncident(incident)
        }
    }

    fun selectIncident(incident: IncidentEntity?) {
        _selectedIncident.value = incident
        _aiReply.value = incident?.aiBriefing ?: ""
        _aiState.value = if (incident?.aiBriefing?.isNotEmpty() == true) "SUCCESS" else "IDLE"
    }

    fun updateIncidentNotesAndStatus(id: Long, updatedNotes: String, updatedStatus: String) {
        viewModelScope.launch {
            val current = _selectedIncident.value
            if (current != null && current.id == id) {
                val updated = current.copy(
                    analystNotes = updatedNotes,
                    status = updatedStatus
                )
                repository.updateIncident(updated)
                _selectedIncident.value = updated
            }
        }
    }

    fun resolveIncident(incident: IncidentEntity, statusResult: String) {
        viewModelScope.launch {
            val updated = incident.copy(status = statusResult)
            repository.updateIncident(updated)
            if (_selectedIncident.value?.id == incident.id) {
                _selectedIncident.value = updated
            }
        }
    }

    fun purgeIncident(id: Long) {
        viewModelScope.launch {
            repository.deleteIncidentById(id)
            if (_selectedIncident.value?.id == id) {
                _selectedIncident.value = null
                _aiReply.value = ""
                _aiState.value = "IDLE"
            }
        }
    }

    // --- Gemini AI Threat Operations ---

    fun triggerAiAnalystBriefing(incident: IncidentEntity) {
        _aiState.value = "LOADING"
        _aiReply.value = ""
        viewModelScope.launch {
            try {
                val briefing = GeminiThreatAnalyst.getThreatPlaybook(
                    attackType = incident.attackType,
                    attackerIp = incident.attackerIp,
                    attackerCountry = incident.attackerCountry,
                    targetIp = incident.targetIp,
                    targetCountry = incident.targetCountry,
                    port = incident.port,
                    threatLevel = incident.threatLevel
                )
                
                // Write briefing analysis state back to SQLite via Room so it registers permanently!
                val updatedIncident = incident.copy(aiBriefing = briefing)
                repository.logIncident(updatedIncident)
                
                if (_selectedIncident.value?.id == incident.id) {
                    _selectedIncident.value = updatedIncident
                    _aiReply.value = briefing
                    _aiState.value = "SUCCESS"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _aiState.value = "ERROR"
                _aiReply.value = "Security Intelligence connection interrupted: ${e.localizedMessage}"
            }
        }
    }
}
