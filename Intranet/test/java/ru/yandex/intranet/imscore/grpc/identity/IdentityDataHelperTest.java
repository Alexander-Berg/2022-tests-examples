package ru.yandex.intranet.imscore.grpc.identity;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;

import ru.yandex.intranet.imscore.proto.identity.Identity;
import ru.yandex.intranet.imscore.proto.identity.IdentityData;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.GRPC_STRING_DEFAULT;
import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertEqualsWithoutUpdateDate;

/**
 * Identity data helper test
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
public final class IdentityDataHelperTest {
    private IdentityDataHelperTest() {
    }

    public static void assertEquals(Set<Identity> identity, Set<Identity> other) {
        assertEquals(identity, other, true);
    }


    public static void assertEquals(Set<Identity> identity, Set<Identity> other, boolean withUpdateTime) {
        Set<Identity> identityWithoutAdditionalDataSet = identity.stream()
                .map(IdentityDataHelperTest::toIdentityWithoutJson)
                .collect(Collectors.toSet());
        Set<Identity> otherWithoutAdditionalDataSet = other.stream()
                .map(IdentityDataHelperTest::toIdentityWithoutJson)
                .collect(Collectors.toSet());
        if (withUpdateTime) {
            Assertions.assertEquals(identityWithoutAdditionalDataSet, otherWithoutAdditionalDataSet);
        } else {
            assertEqualsWithoutUpdateDate(identityWithoutAdditionalDataSet, otherWithoutAdditionalDataSet);
        }
        Map<String, String> additionalDataByIdentityId = identity.stream()
                .collect(Collectors.toMap(Identity::getId, value -> value.getData().getAdditionalData()));
        Map<String, String> additionalDataByOtherId = other.stream()
                .collect(Collectors.toMap(Identity::getId, value -> value.getData().getAdditionalData()));

        additionalDataByIdentityId.keySet()
                .forEach(id -> Assertions.assertEquals(toJsonObject(additionalDataByIdentityId.get(id)),
                        toJsonObject(additionalDataByOtherId.get(id))));
    }

    public static void assertEquals(Identity identity, Identity other) {
        Assertions.assertEquals(toIdentityWithoutJson(identity), toIdentityWithoutJson(other));
        Assertions.assertEquals(toJsonObject(identity.getData().getAdditionalData()),
                toJsonObject(other.getData().getAdditionalData()));
    }

    public static Identity toIdentityWithoutJson(Identity identity) {
        if (identity.hasData()) {
            IdentityData identityData = toIdentityDataWithoutJson(identity.getData());
            return Identity.newBuilder(identity)
                    .setData(identityData)
                    .build();
        }
        return identity;
    }

    public static IdentityData toIdentityDataWithoutJson(IdentityData identityData) {
        String additionalData = identityData.getAdditionalData();
        if (additionalData.equals(GRPC_STRING_DEFAULT)) {
            return identityData;
        }

        return IdentityData.newBuilder(identityData)
                .clearAdditionalData()
                .build();
    }

    public static JsonObject toJsonObject(String json) {
        if (json.equals(GRPC_STRING_DEFAULT)) {
            return null;
        }

        return JsonParser.parseString(json).getAsJsonObject();
    }
}
