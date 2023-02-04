package ru.yandex.qe.dispenser.standalone

import ru.yandex.qe.dispenser.api.v1.*
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse
import ru.yandex.qe.dispenser.domain.Project
import ru.yandex.qe.dispenser.ws.d.DLegacyApi
import ru.yandex.qe.dispenser.ws.reqbody.MaxValueBody
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

/**
 * Mock D legacy API.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 26-04-2022
 */
class MockDLegacyApi : DLegacyApi {
    override fun update(
        projectKey: String,
        serviceKey: String,
        resourceKey: String,
        quotaSpecKey: String,
        uid: String,
        maxValue: MaxValueBody
    ): DiQuota {
        return DiQuota.builder(
            DiQuotaSpec.Builder(quotaSpecKey)
                .resource(
                    DiResource.withKey(resourceKey)
                        .withName("-")
                        .withType(DiResourceType.PROCESSOR)
                        .build()
                )
                .description("-")
                .type(DiQuotaType.ABSOLUTE)
                .build(),
            DiProject
                .withKey(projectKey)
                .withName("-")
                .build()
        )
            .max(DiAmount.of(200, maxValue.unit))
            .actual(DiAmount.of(200, maxValue.unit))
            .ownMax(DiAmount.of(200, maxValue.unit))
            .ownActual(DiAmount.of(200, maxValue.unit))
            .statisticsLink("uid: $uid")
            .segments(setOf())
            .build()
    }

    override fun filterProjects(
        leaf: Boolean,
        responsibleLogins: List<String>,
        memberLogins: List<String>,
        projectKeys: List<String>,
        showPersons: Boolean,
        cacheDisabled: Boolean,
        fieldsParam: String?,
        fieldParam: Set<String>?
    ): Response {
        return Response.ok(
            DiListResponse(
                projectKeys.map { projectKey ->
                    Project.withKey(projectKey)
                        .build()
                        .toView(false)
                }
            )
        ).build()
    }

    override fun read(
        resourceParams: List<String>,
        entitySpecParams: List<String>,
        segmentKeys: Set<String>,
        serviceKeys: Set<String>,
        projectAbcServiceSlugs: Set<String>,
        memberLogins: Set<String>,
        leafProjects: Boolean,
        order: String?
    ): DiListResponse<DiQuotaLightView> {
        throw WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(null).build())
    }
}
