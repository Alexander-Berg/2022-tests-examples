package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class ReusableUsingRuleTest {

    @Test
    fun `Reports issue if useCase is Reusable`() {
        val findings = """
         
        @Reusable
        class DeliveryLocalityUseCase @Inject constructor(
            private val geoCoordinatesMapper: GeoCoordinatesMapper,
            private val regionTypeMapper: RegionTypeMapper
        ) {
        }
            
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The class DeliveryLocalityUseCase cannot be Reusable"
        )
    }

    @Test
    fun `Reports issue if repository is Reusable`() {
        val findings = """
         
        @Reusable
        class DeliveryLocalityRepository @Inject constructor(
            private val geoCoordinatesMapper: GeoCoordinatesMapper,
            private val regionTypeMapper: RegionTypeMapper
        ) {
        }
            
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The class DeliveryLocalityRepository cannot be Reusable"
        )
    }

    @Test
    fun `Do not reports issue if mapper is Reusable`() {
        val findings = """
            
        @Reusable
        class GeoCoordinatesMapper @Inject constructor(
            private val geoCoordinatesMapper: GeoCoordinatesMapper,
            private val regionTypeMapper: RegionTypeMapper
        ) {
        }
        
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Do not reports issue if formatter is Reusable`() {
        val findings = """
            
        @Reusable
        class GeoCoordinatesFormatter @Inject constructor(
            private val geoCoordinatesMapper: GeoCoordinatesMapper,
            private val regionTypeMapper: RegionTypeMapper
        ) {
        }
        
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Do not reports issue if stateless class is Reusable`() {
        val findings = """
            
        @Reusable
        class GeoCoordinatesSOmeClass @Inject constructor() {
        
        }
        
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    private fun String.toFindings(): List<Finding> = ReusableUsingRule().lint(trimIndent())
}