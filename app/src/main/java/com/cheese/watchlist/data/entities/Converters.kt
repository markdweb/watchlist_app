package com.cheese.watchlist.data.entities

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromWatchStatus(status: WatchStatus): String = status.name

    @TypeConverter
    fun toWatchStatus(value: String): WatchStatus = WatchStatus.valueOf(value)

    @TypeConverter
    fun fromWatchType(type: WatchType): String = type.name

    @TypeConverter
    fun toWatchType(value: String): WatchType = WatchType.valueOf(value)

    @TypeConverter
    fun fromTaggableType(type: TaggableType): String = type.name

    @TypeConverter
    fun toTaggableType(value: String): TaggableType = TaggableType.valueOf(value)
}
