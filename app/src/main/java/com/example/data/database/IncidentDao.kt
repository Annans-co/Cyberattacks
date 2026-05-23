package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.data.model.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity): Long

    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Delete
    suspend fun deleteIncident(incident: IncidentEntity)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncidentById(id: Long)
}
