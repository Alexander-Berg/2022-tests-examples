package ru.auto.lintrules.issues

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.lintrules.lintAsKotlinFile

@RunWith(AllureRunner::class) class SerializableShouldBeTransitiveTest {
    @Test
    fun shouldNotReportClassesThatAreNotSerializable() {
        @Language("Kt")
        val fileContent = """
        package ru.auto.lintrules.issues
        class Person(val age: Int, name: Name)
        class Name(val firstName: String, val secondName: String)
        """.trimIndent()

        fileContent.lintAsKotlinFile(SerializableShouldBeTransitiveIssue)
            .expectClean()
    }


    @Test
    fun shouldNotReportClassesThatHaveOnlyPrimitiveFields() {
        @Language("Kt")
        val fileContent = """
        package ru.auto.lintrules.issues
        import java.io.Serializable
        class Person(val age: Int, val name: String) : Serializable
        object SomeObject
        """.trimIndent()

        fileContent.lintAsKotlinFile(SerializableShouldBeTransitiveIssue)
            .expectClean()
    }

    @Test
    fun shouldNotReportClassesThatUseSerializables() {
        @Language("Kt")
        val fileContent = """
        package ru.auto.lintrules.issues
        import java.io.Serializable
        class Person(val name: Name, val some: SomeObject): Serializable
        class Name(val first: String, second: String): Serializable
        object SomeObject: Serializable
        """.trimIndent()

        fileContent.lintAsKotlinFile(SerializableShouldBeTransitiveIssue)
            .expectClean()
    }

    @Test
    fun shouldNotReportClassesThatUseFieldWhoInheritFromCollection() {
        @Language("Kt")
        val fileContent = """
        package ru.auto.lintrules.issues
        import java.io.Serializable
        import kotlin.collections.Map

        class Person(val some: Map<*,*>): Serializable
        """.trimIndent()

        fileContent.lintAsKotlinFile(SerializableShouldBeTransitiveIssue)
            .expectClean()
    }

    @Test
    fun shouldNotReportClassesThatUseFieldWhoInheritFromSerializableClasses() {
        @Language("Kt")
        val fileContent = """
        package ru.auto.lintrules.issues
        import java.io.Serializable
        class Person(val some: SomeObject): Serializable
        open class Name(val first: String, second: String): Serializable
        object SomeObject: Name("John", "Doe")
        """.trimIndent()

        fileContent.lintAsKotlinFile(SerializableShouldBeTransitiveIssue)
            .expectClean()
    }

    @Test
    fun shouldNotReportClassesThatUseTransientNonSerializableField() {
        @Language("Kt")
        val fileContent = """
        package ru.auto.lintrules.issues
        import java.io.Serializable
        class Person(@Transient val some: SomeObject): Serializable
        object SomeObject
        """.trimIndent()

        fileContent.lintAsKotlinFile(SerializableShouldBeTransitiveIssue)
            .expectClean()
    }


    @Test
    fun shouldNotReportClassesWithCompanionObjects() {
        @Language("Kt")
        val fileContent = """
        package ru.auto.lintrules.issues
        import java.io.Serializable
        class Person(@Transient val some: SomeObject): Serializable {
            companion object {
                val something: Int
            }
        }
        """.trimIndent()

        fileContent.lintAsKotlinFile(SerializableShouldBeTransitiveIssue)
            .expectClean()
    }
}
