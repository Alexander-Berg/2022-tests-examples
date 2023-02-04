package ru.yandex.intranet.imscore.grpc.common

import com.google.protobuf.Timestamp
import ru.yandex.intranet.imscore.grpc.common.IdentityTypeSourceTestData.STAFF_IDENTITY_TYPE_SOURCE
import ru.yandex.intranet.imscore.proto.identity.Identity
import ru.yandex.intranet.imscore.proto.identity.IdentityData
import ru.yandex.intranet.imscore.proto.identityType.IdentityType
import java.text.SimpleDateFormat

/**
 * Example values for tests
 *
 * @author Mustakayev Marat <mmarat248@yandex-team.ru>
 */
class TestData {
    companion object {
        val identityType: IdentityType = IdentityType.newBuilder()
            .setId("test_type_id")
            .setIsGroup(false)
            .setSourceId(STAFF_IDENTITY_TYPE_SOURCE.id)
            .build()
        val identityGroupType: IdentityType = IdentityType.newBuilder()
            .setId("test_type_id2")
            .setIsGroup(true)
            .setSourceId(STAFF_IDENTITY_TYPE_SOURCE.id)
            .build()
        private val identityTimestamp: Timestamp =
            Timestamp.newBuilder().setSeconds(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .parse("2022-05-13 07:02:51")
                    .toInstant().epochSecond
            ).build()

        val identity: Identity = Identity.newBuilder()
            .setId("0817ba5c-e322-48cf-b716-984d4da2193e")
            .setExternalId("test_ext_id")
            .setType(identityType)
            .setCreatedAt(identityTimestamp)
            .setModifiedAt(identityTimestamp)
            .build()
        val identityData: IdentityData = IdentityData.newBuilder()
            .setSlug("slug_test")
            .setName("name_test")
            .setLastname("lastname_test")
            .setPhone("phone_test")
            .setEmail("email_test")
            .setAdditionalData("{\"data\": 1}")
            .build()
        val identityWithData: Identity = Identity.newBuilder()
            .setId("0817ba5c-e322-48cf-b716-984d4da2193e")
            .setExternalId("test_ext_id")
            .setType(identityType)
            .setData(identityData)
            .setCreatedAt(identityTimestamp)
            .setModifiedAt(identityTimestamp)
            .build()

        val identityGroup1: Identity = Identity.newBuilder()
            .setId("66e0fee4-ef5e-4a22-92bd-05c80ced4d29")
            .setExternalId("test_ext_group1")
            .setType(identityGroupType)
            .setCreatedAt(identityTimestamp)
            .setModifiedAt(identityTimestamp)
            .build()
        val identityGroup1Data: IdentityData = IdentityData.newBuilder()
            .setSlug("incididunt ea dolore ut")
            .build()
        val identityGroup1WithData: Identity = Identity.newBuilder()
            .setId("66e0fee4-ef5e-4a22-92bd-05c80ced4d29")
            .setExternalId("test_ext_group1")
            .setType(identityGroupType)
            .setData(identityGroup1Data)
            .setCreatedAt(identityTimestamp)
            .setModifiedAt(identityTimestamp)
            .build()
        val identityGroup2: Identity = Identity.newBuilder()
            .setId("77e0fee4-ef5e-4a22-92bd-05c80ced4d77")
            .setExternalId("test_ext_group2")
            .setType(identityGroupType)
            .setCreatedAt(identityTimestamp)
            .setModifiedAt(identityTimestamp)
            .build()
        val identityGroup3: Identity = Identity.newBuilder()
            .setId("88e0fee4-ef5e-4a22-92bd-05c80ced4d88")
            .setExternalId("test_ext_group3")
            .setType(identityGroupType)
            .setCreatedAt(identityTimestamp)
            .setModifiedAt(identityTimestamp)
            .build()
        val identityGroup4: Identity = Identity.newBuilder()
            .setId("99e0fee4-ef5e-4a22-92bd-05c80ced4d99")
            .setExternalId("test_ext_group4")
            .setType(identityGroupType)
            .setCreatedAt(identityTimestamp)
            .setModifiedAt(identityTimestamp)
            .build()
        val identityGroup4SubGroup1: Identity = Identity.newBuilder()
            .setId("9910fee4-ef5e-4a22-92bd-05c80ced4d99")
            .setParentId("99e0fee4-ef5e-4a22-92bd-05c80ced4d99")
            .setExternalId("test_ext_group5")
            .setType(identityGroupType)
            .setCreatedAt(identityTimestamp)
            .setModifiedAt(identityTimestamp)
            .build()
    }
}
