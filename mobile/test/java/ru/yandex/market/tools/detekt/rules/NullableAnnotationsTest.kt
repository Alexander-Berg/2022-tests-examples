package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class NullableAnnotationsTest {

    @Test
    fun `Expect warning when used javaxAnnotationNonnull annotation without import`() {
        val codeString = """
                    @javax.annotation.Nonnull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"NonNull\" annotation!")
    }

    @Test
    fun `Expect warning when used ioReactivexAnnotationsNonNull annotation without import `() {
        val codeString = """
                    @io.reactivex.annotations.NonNull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"NonNull\" annotation!")
    }

    // If we will check jetbrains annotations in kotlin we will get a lot of warnings because all variable in kotlin
    // are annotated by jetbrains annotations
    @Test
    fun `Expect clean when used orgJetbrainsAnnotationsNotNull annotation without import`() {
        val codeString = """
                    @org.jetbrains.annotations.NotNull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect warning when used androidSupportAnnotationNonNull annotation without import`() {
        val codeString = """
                    @android.support.annotation.NonNull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"NonNull\" annotation!")
    }

    @Test
    fun `Expect warning when used orgCheckerframeworkCheckerNullnessQualNonNull annotation without import`() {
        val codeString = """
                    @org.checkerframework.checker.nullness.qual.NonNull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"NonNull\" annotation!")
    }

    @Test
    fun `Expect warning when used javaxAnnotationNullable annotation without import`() {
        val codeString = """
                    @javax.annotation.Nullable
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"Nullable\" annotation!")
    }

    @Test
    fun `Expect warning when used ioReactivexAnnotationsNullable annotation without import`() {
        val codeString = """
                    @io.reactivex.annotations.Nullable
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"Nullable\" annotation!")
    }

    // If we will check jetbrains annotations in kotlin we will get a lot of warnings because all variable in kotlin
    // are annotated by jetbrains annotations
    @Test
    fun `Expect clean when used orgJetbrainsAnnotationsNullable annotation without import`() {
        val codeString = """
                    @org.jetbrains.annotations.Nullable
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect warning when used androidSupportAnnotationNullable annotation without import`() {
        val codeString = """
                    @android.support.annotation.Nullable
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"Nullable\" annotation!")
    }

    // If we will check jetbrains annotations in kotlin we will get a lot of warnings because all variable in kotlin
    // are annotated by jetbrains annotations
    @Test
    fun `Expect clean when used orgJetbrainsAnnotationsNonNls annotation without import`() {
        val codeString = """
                    @org.jetbrains.annotations.NonNls
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect warning when used javaxAnnotationNonnull annotation`() {
        val codeString = """
                    import javax.annotation.Nonnull
                    
                    @Nonnull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"NonNull\" annotation!")
    }

    @Test
    fun `Expect warning when used ioReactivexAnnotationsNonNull annotation`() {
        val codeString = """
                    import io.reactivex.annotations.NonNull
                    
                    @NonNull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"NonNull\" annotation!")
    }

    // If we will check jetbrains annotations in kotlin we will get a lot of warnings because all variable in kotlin
    // are annotated by jetbrains annotations
    @Test
    fun `Expect clean when used orgJetbrainsAnnotationsNotNull annotation`() {
        val codeString = """
                    import org.jetbrains.annotations.NotNull
                    
                    @NotNull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect warning when used androidSupportAnnotationNonNull annotation`() {
        val codeString = """
                    import android.support.annotation.NonNull
                    
                    @NonNull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"NonNull\" annotation!")
    }

    @Test
    fun `Expect warning when used orgCheckerframeworkCheckerNullnessQualNonNull annotation`() {
        val codeString = """
                    import org.checkerframework.checker.nullness.qual.NonNull
                    
                    @NonNull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"NonNull\" annotation!")
    }

    @Test
    fun `Expect warning when used javaxAnnotationNullable annotation`() {
        val codeString = """
                    import javax.annotation.Nullable
                    
                    @Nullable
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"Nullable\" annotation!")
    }

    @Test
    fun `Expect warning when used ioReactivexAnnotationsNullable annotation`() {
        val codeString = """
                    import io.reactivex.annotations.Nullable
                    
                    @Nullable
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"Nullable\" annotation!")
    }

    // If we will check jetbrains annotations in kotlin we will get a lot of warnings because all variable in kotlin
    // are annotated by jetbrains annotations
    @Test
    fun `Expect clean when used orgJetbrainsAnnotationsNullable annotation`() {
        val codeString = """
                    import org.jetbrains.annotations.Nullable
                    
                    @Nullable
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect warning when used androidSupportAnnotationNullable annotation`() {
        val codeString = """
                    import android.support.annotation.Nullable
                    
                    @Nullable
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"Nullable\" annotation!")
    }

    // If we will check jetbrains annotations in kotlin we will get a lot of warnings because all variable in kotlin
    // are annotated by jetbrains annotations
    @Test
    fun `Expect clean when used orgJetbrainsAnnotationsNonNls annotation`() {
        val codeString = """
                    import org.jetbrains.annotations.NonNls
                    
                    @NonNls
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect warning when used javaxAnnotationNonnull annotation more than once time`() {
        val codeString = """
                    import javax.annotation.Nonnull

                    @Nonnull
                    class TestClass {
                        @Nonnull val cur = 10
                        
                        fun setValue(@Nonnull value: Int) {
                            cur = value
                        }
                    }
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(3)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"NonNull\" annotation!")
        Assertions.assertThat(findings[1].message).isEqualTo("Not recommended \"NonNull\" annotation!")
        Assertions.assertThat(findings[2].message).isEqualTo("Not recommended \"NonNull\" annotation!")
    }

    @Test
    fun `Expect clean when used androidxAnnotationNonNull annotation`() {
        val codeString = """
                    import androidx.annotation.NonNull
                    
                    @NonNull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect clean when used androidxAnnotationNullable annotation`() {
        val codeString = """
                    import androidx.annotation.Nullable
                    
                    @Nullable
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect clean when used androidxAnnotationNonNull annotation without import`() {
        val codeString = """
                    @androidx.annotation.NonNull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect clean when used androidxAnnotationNullable annotation without import`() {
        val codeString = """
                    @androidx.annotation.Nullable
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect quick fix when used javaxAnnotationNonnull annotation with correct import`() {
        val codeString = """
                    import javax.annotation.Nonnull
                    import androidx.annotation.NonNull
                    
                    @Nonnull
                    class TestClass
                    """
        val findings = NullableAnnotations().lint(
            codeString.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo("Not recommended \"NonNull\" annotation!")
    }

}