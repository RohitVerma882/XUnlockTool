package com.xunlocktool.utils

class HttpQuery : LinkedHashMap<String?, Any?>() {
    override fun put(key: String?, value: Any?): Any? {
        return super.put(key, value ?: "null")
    }

    fun sorted(): HttpQuery {
        val sortedHQ = HttpQuery()
        this.entries
            .sortedBy { it.key }
            .forEach { entry -> sortedHQ[entry.key] = entry.value }
        return sortedHQ
    }

    fun toEncodedString(encode: Boolean): String {
        val builder = StringBuilder()
        for (entry in super.entries) {
            builder.apply {
                append(entry.key)
                append('=')
                if (encode) {
                    append(HttpUtils.encodeUrl(entry.value.toString()))
                } else {
                    append(entry.value.toString())
                }
                append('&')
            }
        }
        builder.setLength(builder.length - 1)
        return builder.toString()
    }

    override fun toString(): String = toEncodedString(false)
}