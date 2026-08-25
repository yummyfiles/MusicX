package com.yummyfiles.musicx.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
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
public class FavoriteDao_Impl(
  __db: RoomDatabase,
) : FavoriteDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFavoriteEntity: EntityInsertAdapter<FavoriteEntity>

  private val __deleteAdapterOfFavoriteEntity: EntityDeleteOrUpdateAdapter<FavoriteEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFavoriteEntity = object : EntityInsertAdapter<FavoriteEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `favorites` (`songId`) VALUES (?)"

      protected override fun bind(statement: SQLiteStatement, entity: FavoriteEntity) {
        statement.bindLong(1, entity.songId)
      }
    }
    this.__deleteAdapterOfFavoriteEntity = object : EntityDeleteOrUpdateAdapter<FavoriteEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `favorites` WHERE `songId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: FavoriteEntity) {
        statement.bindLong(1, entity.songId)
      }
    }
  }

  public override suspend fun insertFavorite(favorite: FavoriteEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfFavoriteEntity.insert(_connection, favorite)
  }

  public override suspend fun deleteFavorite(favorite: FavoriteEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfFavoriteEntity.handle(_connection, favorite)
  }

  public override fun getAllFavoriteIds(): Flow<List<Long>> {
    val _sql: String = "SELECT songId FROM favorites"
    return createFlow(__db, false, arrayOf("favorites")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<Long> = mutableListOf()
        while (_stmt.step()) {
          val _item: Long
          _item = _stmt.getLong(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteFavoriteById(songId: Long) {
    val _sql: String = "DELETE FROM favorites WHERE songId = ?"
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
