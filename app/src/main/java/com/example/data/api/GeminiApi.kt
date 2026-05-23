package com.example.data.api

import com.example.data.model.GenerateContentRequest
import com.example.data.model.GenerateContentResponse
import com.example.data.model.MoshiContent
import com.example.data.model.MoshiPart
import com.example.data.model.MoshiGenerationConfig
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Body
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)
}

object GeminiThreatAnalyst {
    
    suspend fun getThreatPlaybook(
        attackType: String,
        attackerIp: String,
        attackerCountry: String,
        targetIp: String,
        targetCountry: String,
        port: Int,
        threatLevel: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Graceful safety check for empty key / default placeholders
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
            return@withContext getLocalFallbackPlaybook(attackType, port, threatLevel, attackerIp, attackerCountry)
        }

        val prompt = """
            You are "CyberSentry AI Analyst", an elite, fully automated Tier-3 incident handler.
            Analyze the following real-time detected cyber threat alert and produce an Incident Briefing Report.
            
            ALERT DETAILS:
            - Threat Vector Type: $attackType
            - Threat Level: $threatLevel
            - Target Interface Port: $port
            - Attacker Origin: $attackerIp ($attackerCountry)
            - Impact Scope: Target IP $targetIp ($targetCountry)
            
            Format your final response with these 4 clear tactical sections in markdown style:
            1. **EXECUTIVE EXECUTIVE SUMMARY**: 2-3 sentence technical overview of what this attack looks to achieve.
            2. **RISK ASSESSMENT & BLAST RADIUS**: Details of what might be compromised if this port / vector is fully pierced.
            3. **IMMEDIATE REMEDIATION STEPS**: Specific actions on firewall (IP tables), port sealing, process termination, secret rotation.
            4. **SEC ENG RECOMMENDATION**: Long-term hardening playbook (e.g., zero trust segmentation, IDS signature setup, MFA flags).
            
            Use professional, highly technical but crisp cyber security officer tone. Keep it highly action-focused and readable with clear bold lines.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(MoshiContent(parts = listOf(MoshiPart(text = prompt)))),
            systemInstruction = MoshiContent(parts = listOf(MoshiPart(text = "You are a cyber security analyst command system. You write precise, bulletproof, analytical briefing briefs."))),
            generationConfig = MoshiGenerationConfig(temperature = 0.2f, maxOutputTokens = 800)
        )

        try {
            val response = GeminiRetrofitClient.apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "AI Threat Intelligence temporary signal degradation: No telemetry response returned."
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful fallback on network exception or quota limit exhaustion
            getLocalFallbackPlaybook(attackType, port, threatLevel, attackerIp, attackerCountry) + 
                "\n\n*(Note: Signal is temporarily generated using local ruleset due to connection latency: ${e.localizedMessage})*"
        }
    }

    private fun getLocalFallbackPlaybook(
        attackType: String,
        port: Int,
        threatLevel: String,
        attackerIp: String,
        attackerCountry: String
    ): String {
        return """
            ### 🚨 CYBERSENTRY ALERT: DETECTED TACTICAL BRIEFING (LOCAL INSTANCE)
            
            **Executive Summary**:
             Sentry node detected an active, malicious sequence matching known signatures for dynamic **$attackType** targeting network ingress on Port **$port**. Origin tracing suggests command vectors residing at IP address **$attackerIp** location **$attackerCountry**.
            
            **Blast Radius Assessment**:
            - **Ingress Port**: TCP/UDP $port.
            - **Criticality Level**: $threatLevel intensity. Host service under threat of system manipulation, service disruption, or data unauthorized traversal.
            - **Blast Radius**: High potential for horizontal traversal across neighbouring active routing segments.
            
            **Tactical Action Playbook (Immediate Mitigations)**:
            1. **Firewall Ban**: Push instant block of incoming IPv4 packets from source `$attackerIp` to edge firewalls.
            2. **Port Quarantining**: Seal Port `$port` temporarily if traffic does not map to verified internal service layers.
            3. **Process Auditing**: Search active kernel memory segments for active processes bound to Port `$port`.
            4. **Token Revocation**: Force credentials rotation for keys exposed to high intensity queries.
            
            **Defensive Hardening Advice**:
            Transition connection targets to Cloudflare / AWS edge routing. Enforce ingress rate limit bounds of max 50 requests/sec. Install an intrusion prevention layer mapping dynamic snort signatures.
        """.trimIndent()
    }
}
