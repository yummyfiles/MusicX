package com.yummyfiles.musicx.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class LyricDao_Impl(
  __db: RoomDatabase,
) : LyricDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfLyricEntity: EntityInsertAdapter<LyricEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfLyricEntity = object : EntityInsertAdapter<LyricEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `lyrics` (`songId`,`lyrics`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LyricEntity) {
        statement.bindLong(1, entity.songId)
        statement.bindText(2, entity.lyrics)
      }
    }
  }

  public override suspend fun insertLyrics(lyric: LyricEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfLyricEntity.insert(_connection, lyric)
  }

  public override suspend fun getLyricsForSong(songId: Long): String? {
    val _sql: String = "SELECT lyrics FROM lyrics WHERE songId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, songId)
        val _result: String?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getText(0)
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteLyricsById(songId: Long) {
    val _sql: String = "DELETE FROM lyrics WHERE songId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, songId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
