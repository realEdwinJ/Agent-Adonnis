package com.adonnis.app.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

/**
 * Type converters for Room to store List types as JSON strings.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        val json = JSONArray()
        value.forEach { json.put(it) }
        return json.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val list = mutableListOf<String>()
        val json = JSONArray(value)
        for (i in 0 until json.length()) {
            list.add(json.getString(i))
        }
        return list
    }
}
