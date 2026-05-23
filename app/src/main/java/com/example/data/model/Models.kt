package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

// --- Cyber Attack Tracker - Live Simulation Event ---
data class AttackEvent(
    val id: String,
    val timestamp: Long,
    val attackerIp: String,
    val attackerCountry: String,
    val attackerCountryCode: String,
    val attackerLat: Float,   // Normalized world coordinates (0f to 1f)
    val attackerLon: Float,
    val targetIp: String,
    val targetCountry: String,
    val targetCountryCode: String,
    val targetLat: Float,     // Normalized world coordinates (0f to 1f)
    val targetLon: Float,
    val attackType: String,
    val threatLevel: String,   // LOW, MEDIUM, HIGH, CRITICAL
    val port: Int,
    val severityScore: Int,   // 0 - 100
    val payloadSize: String
)

// --- Incident Entity (Room Database for Local Security Operations) ---
@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val attackerIp: String,
    val attackerCountry: String,
    val attackerCountryCode: String,
    val targetIp: String,
    val targetCountry: String,
    val targetCountryCode: String,
    val attackType: String,
    val threatLevel: String,
    val port: Int,
    val payloadSize: String,
    val status: String, // INVESTIGATING, MITIGATED, FALSE_ALARM, ESCALATED
    val analystNotes: String,
    val playbookName: String,
    val aiBriefing: String // Cached Gemini AI response
)

// --- Gemini AI Rest Request & Response Models (Moshi format) ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<MoshiContent>,
    val systemInstruction: MoshiContent? = null,
    val generationConfig: MoshiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class MoshiContent(
    val parts: List<MoshiPart>
)

@JsonClass(generateAdapter = true)
data class MoshiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class MoshiGenerationConfig(
    val temperature: Float = 0.5f,
    val maxOutputTokens: Int = 1000
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<MoshiCandidate>?
)

@JsonClass(generateAdapter = true)
data class MoshiCandidate(
    val content: MoshiContent?
)
