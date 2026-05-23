package com.example.data.repository

import com.example.data.database.IncidentDao
import com.example.data.model.IncidentEntity
import kotlinx.coroutines.flow.Flow

class ThreatRepository(private val incidentDao: IncidentDao) {

    val allIncidents: Flow<List<IncidentEntity>> = incidentDao.getAllIncidents()

    suspend fun logIncident(incident: IncidentEntity): Long {
        return incidentDao.insertIncident(incident)
    }

    suspend fun updateIncident(incident: IncidentEntity) {
        incidentDao.updateIncident(incident)
    }

    suspend fun deleteIncident(incident: IncidentEntity) {
        incidentDao.deleteIncident(incident)
    }

    suspend fun deleteIncidentById(id: Long) {
        incidentDao.deleteIncidentById(id)
    }
}
