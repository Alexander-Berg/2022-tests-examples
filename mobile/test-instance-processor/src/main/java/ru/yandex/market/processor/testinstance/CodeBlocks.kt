package ru.yandex.market.processor.testinstance

import androidx.annotation.RestrictTo
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.math.BigDecimal
import java.util.GregorianCalendar

object CodeBlocks {

    private const val PACKAGE_KOTLIN_COLLECTIONS = "kotlin.collections"
    private const val MEMBER_LITERAL = "%M(%L)"
    private const val MEMBER = "%M()"

    val restrictToTestsAnnotation =
        AnnotationSpec.builder(RestrictTo::class)
            .addMember(RestrictTo.Scope.TESTS.toEnumLiteral())
            .build()

    val jvmOverloadsAnnotation = AnnotationSpec.builder(JvmOverloads::class).build()

    val nullLiteral = CodeBlock.of("null")

    fun jvmNameAnnotation(value: String, useSiteTarget: AnnotationSpec.UseSiteTarget? = null): AnnotationSpec {
        return AnnotationSpec.builder(JvmName::class)
            .useSiteTarget(useSiteTarget)
            .addMember("name = %S", value)
            .build()
    }

    val bigDecimalTen = CodeBlock.of("%T.TEN", BigDecimal::class)

    fun listOf(parameterLiteral: Any): CodeBlock {
        return CodeBlock.of(
            MEMBER_LITERAL,
            MemberName(PACKAGE_KOTLIN_COLLECTIONS, "listOf"),
            parameterLiteral
        )
    }

    val emptyList: CodeBlock
        get() = CodeBlock.of(MEMBER, MemberName(PACKAGE_KOTLIN_COLLECTIONS, "emptyList"))

    fun mapOf(keyLiteral: Any, valueLiteral: Any): CodeBlock {
        return CodeBlock.of(
            "%M(%L to %L)",
            MemberName(PACKAGE_KOTLIN_COLLECTIONS, "mapOf"),
            keyLiteral,
            valueLiteral
        )
    }

    val emptyMap: CodeBlock
        get() = CodeBlock.of(MEMBER, MemberName(PACKAGE_KOTLIN_COLLECTIONS, "emptyMap"))

    fun setOf(parameterLiteral: Any): CodeBlock {
        return CodeBlock.of(
            MEMBER_LITERAL,
            MemberName(PACKAGE_KOTLIN_COLLECTIONS, "setOf"),
            parameterLiteral
        )
    }

    val emptySet: CodeBlock
        get() = CodeBlock.of(MEMBER, MemberName(PACKAGE_KOTLIN_COLLECTIONS, "emptySet"))

    fun returnStatement(returnLiteral: Any): CodeBlock {
        return CodeBlock.of("return %L", returnLiteral)
    }

    fun constructorInvocation(typeName: TypeName, parameters: Iterable<CodeBlock>): CodeBlock {
        return CodeBlock.of("%T(%L)", typeName, parameters.joinToString(separator = ", "))
    }

    fun enumLiteral(type: TypeName, literal: CharSequence): CodeBlock {
        return CodeBlock.of("%T.%L", type, literal)
    }

    fun newGregorianCalendar(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int,
        second: Int
    ): CodeBlock {

        return CodeBlock.of(
            "%T(%L, %L, %L, %L, %L, %L)",
            GregorianCalendar::class.asTypeName(),
            year,
            month,
            dayOfMonth,
            hourOfDay,
            minute,
            second
        )
    }
}