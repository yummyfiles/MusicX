package com.example.musicx.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.musicx.data.local.dao.IgnoredSongDao
import com.example.musicx.data.local.dao.LibrarySongDao
import com.example.musicx.data.local.dao.LikedSongDao
import com.example.musicx.data.local.dao.MetadataOverrideDao
import com.example.musicx.data.local.dao.PlaylistDao
import com.example.musicx.data.local.entity.IgnoredSong
import com.example.musicx.data.local.entity.LibrarySong
import com.example.musicx.data.local.entity.LikedSong
import com.example.musicx.data.local.entity.MetadataOverride
import com.example.musicx.data.local.entity.Playlist

@Database(entities = [Playlist::class, MetadataOverride::class, LibrarySong::class, IgnoredSong::class, LikedSong::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun metadataOverrideDao(): MetadataOverrideDao
    abstract fun librarySongDao(): LibrarySongDao
    abstract fun ignoredSongDao(): IgnoredSongDao
    abstract fun likedSongDao(): LikedSongDao
}
