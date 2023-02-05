package com.yandex.metrica.profile

class Attribute {
    companion object {
        @JvmStatic
        fun customString(key: String): StringAttribute {
            return StringAttribute(key)
        }

        @JvmStatic
        fun customBoolean(key: String): BooleanAttribute {
            return BooleanAttribute(key)
        }
    }
}
