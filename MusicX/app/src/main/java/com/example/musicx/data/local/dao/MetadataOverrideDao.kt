package com.example.musicx.data.local.dao

import androidx.room.*
import com.example.musicx.data.local.entity.MetadataOverride
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataOverrideDao {
    @Query("SELECT * FROM metadata_overrides")
    fun getAllOverrides(): Flow<List<MetadataOverride>>

    @Query("SELECT * FROM metadata_overrides")
    suspend fun getAllOverridesList(): List<MetadataOverride>

    @Query("SELECT * FROM metadata_overrides WHERE songUri = :uri")
    suspend fun getOverrideByUri(uri: String): MetadataOverride?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: MetadataOverride)

    @Query("DELETE FROM metadata_overrides WHERE songUri IN (:uris)")
    suspend fun deleteOverridesByUri(uris: List<String>)
}
