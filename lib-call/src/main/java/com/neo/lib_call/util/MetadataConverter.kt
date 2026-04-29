package com.neo.lib_call.util

internal object MetadataConverter {
  fun toHashMap(metadata: Map<String, String>): HashMap<String, String> = HashMap(metadata)

  @Suppress("UNCHECKED_CAST")
  fun fromSerializable(value: Any?): Map<String, String> {
    return when (value) {
      is HashMap<*, *> -> value as? HashMap<String, String> ?: emptyMap()
      else -> emptyMap()
    }
  }
}
