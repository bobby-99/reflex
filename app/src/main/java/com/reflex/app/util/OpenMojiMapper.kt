package com.reflex.app.util

import android.content.Context
import org.json.JSONArray

data class OpenMojiMetadataItem(
    val unicode: String,
    val name: String,
    val category: String
)

object OpenMojiMapper {

    private var cachedMetadata: List<OpenMojiMetadataItem>? = null

    fun loadEmojiMetadata(context: Context): List<OpenMojiMetadataItem> {
        cachedMetadata?.let { return it }

        return try {
            val jsonStr = context.assets.open("openmoji_metadata.json").bufferedReader().use { it.readText() }
            val array = JSONArray(jsonStr)
            val list = mutableListOf<OpenMojiMetadataItem>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    OpenMojiMetadataItem(
                        unicode = obj.getString("unicode"),
                        name = obj.getString("name"),
                        category = obj.getString("category")
                    )
                )
            }

            cachedMetadata = list
            list
        } catch (e: Exception) {
            AppLog.w("OpenMojiMapper", "Failed to load openmoji metadata", e)
            emptyList()
        }
    }

    fun getDrawableResId(context: Context, unicode: String?): Int {
        if (unicode.isNullOrBlank()) return 0
        val cleanUnicode = unicode.trim().lowercase()
            .removePrefix("u+")
            .removePrefix("openmoji_")
        
        return try {
            val resId = context.resources.getIdentifier(
                "openmoji_$cleanUnicode",
                "drawable",
                context.packageName
            )
            resId
        } catch (e: Exception) {
            0
        }
    }
}
