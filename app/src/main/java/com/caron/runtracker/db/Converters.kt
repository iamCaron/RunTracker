package com.caron.runtracker.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.caron.runtracker.domain.Polylines

import java.io.ByteArrayOutputStream


class Converters {

    @TypeConverter
    fun toBitmap(bytes:ByteArray):Bitmap{
        return BitmapFactory.decodeByteArray(bytes,0,bytes.size)
    }


    @TypeConverter
    fun fromBitmap(bmp:Bitmap):ByteArray{
        val outputStream=ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG,100,outputStream)
        return outputStream.toByteArray()
    }
    @TypeConverter
    fun polylinesToJson(polylines: Polylines): String {
        val gson = Gson()
        val type = object : TypeToken<Polylines?>() {}.type
        return gson.toJson(polylines, type)

    }
    @TypeConverter
    fun jsonToPolylines(json: String): Polylines {
        val gson = Gson()
        val type = object : TypeToken<Polylines?>() {}.type
        return gson.fromJson(json, type)
    }
}