package ru.yandex.market.processor.testinstance

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.type.TypeMirror
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

fun CharSequence.toStringLiteral() = CodeBlock.of("%S", this)

fun Any.toLiteral() = CodeBlock.of("%L", this)

fun Any.toLongLiteral() = CodeBlock.of("%LL", this)

fun Any.toFloatLiteral() = CodeBlock.of("%Lf", this)

fun <E : Enum<E>> Enum<E>.toEnumLiteral() = CodeBlock.of("%T.%L", this::class, name)

val WildcardTypeName.firstType: TypeName
    get() = (inTypes + outTypes).first()

fun TypeName.toKotlinType(): TypeName {
    return if (this is ParameterizedTypeName) {
        @Suppress("SpreadOperator")
        (rawType.toKotlinType() as ClassName).parameterizedBy(
            *typeArguments.map { it.toKotlinType() }.toTypedArray()
        )
    } else if (this is WildcardTypeName) {
        firstType.toKotlinType()
    } else {
        val className = JavaToKotlinClassMap.INSTANCE
            .mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()
        if (className == null) {
            this
        } else {
            ClassName.bestGuess(className)
        }
    }
}

fun TypeMirror.asClassName() = asTypeName() as ClassName
