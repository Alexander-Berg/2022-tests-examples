package ru.yandex.market.uikit.raiting

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class RatingBuildHelperTest {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    private val ratingBuilder = RatingBuildHelper()

    @Test
    fun `Assert exception by missing property`() {
        ratingBuilder.addRequiredProperty(null, "property")
        thrown.expect(IllegalStateException::class.java)
        ratingBuilder.validate()
    }

    @Test
    fun `Assert exception by missing property 2`() {
        ratingBuilder.addRequiredProperty(null, "property")
        ratingBuilder.addRequiredProperty("property2", "property2")
        thrown.expect(IllegalStateException::class.java)
        ratingBuilder.validate()
    }

    @Test
    fun `Assert validate`() {
        ratingBuilder.addRequiredProperty("property", "property")
        ratingBuilder.validate()
    }
}