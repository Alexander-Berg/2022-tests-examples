package com.yandex.mobile.realty.utils

import java.lang.reflect.Modifier

/**
 * @author shpigun on 2019-08-26
 */

fun Any.getInstanceFieldsCount(): Int {
    var fieldsCount = 0
    var clazz: Class<*>? = this::class.java
    while (clazz != null && clazz != Object::class.java) {
        clazz.declaredFields.forEach { field ->
            if (!Modifier.isStatic(field.modifiers)) {
                fieldsCount++
            }
        }
        clazz = clazz.superclass
    }
    return fieldsCount
}
