package com.yummyfiles.musicx.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PlaylistDao_Impl(
  __db: RoomDatabase,
) : PlaylistDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPlaylistEntity: EntityInsertAdapter<PlaylistEntity>

  private val __deleteAdapterOfPlaylistEntity: EntityDeleteOrUpdateAdapter<PlaylistEntity>

  private val __updateAdapterOfPlaylistEntity: EntityDeleteOrUpdateAdapter<PlaylistEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfPlaylistEntity = object : EntityInsertAdapter<PlaylistEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `playlists` (`id`,`name`,`songUrisJson`) VALUES (nullif(?, 0),?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PlaylistEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.songUrisJson)
      }
    }
    this.__deleteAdapterOfPlaylistEntity = object : EntityDeleteOrUpdateAdapter<PlaylistEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `playlists` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PlaylistEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfPlaylistEntity = object : EntityDeleteOrUpdateAdapter<PlaylistEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `playlists` SET `id` = ?,`name` = ?,`songUrisJson` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PlaylistEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.songUrisJson)
        statement.bindLong(4, entity.id)
      }
    }
  }

  public override suspend fun insertPlaylist(playlist: PlaylistEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPlaylistEntity.insert(_connection, playlist)
  }

  public override suspend fun deletePlaylist(playlist: PlaylistEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfPlaylistEntity.handle(_connection, playlist)
  }

  public override suspend fun updatePlaylist(playlist: PlaylistEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfPlaylistEntity.handle(_connection, playlist)
  }

  public override fun getAllPlaylists(): Flow<List<PlaylistEntity>> {
    val _sql: String = "SELECT * FROM playlists"
    return createFlow(__db, false, arrayOf("playlists")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfSongUrisJson: Int = getColumnIndexOrThrow(_stmt, "songUrisJson")
        val _result: MutableList<PlaylistEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PlaylistEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpSongUrisJson: String
          _tmpSongUrisJson = _stmt.getText(_columnIndexOfSongUrisJson)
          _item = PlaylistEntity(_tmpId,_tmpName,_tmpSongUrisJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
