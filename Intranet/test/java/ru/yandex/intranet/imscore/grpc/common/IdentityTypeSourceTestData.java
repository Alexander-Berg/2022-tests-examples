package ru.yandex.intranet.imscore.grpc.common;


import java.util.Set;

import ru.yandex.intranet.imscore.proto.identityTypeSource.IdentityTypeSource;

/**
 * Identity type source test data.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("unused")
public final class IdentityTypeSourceTestData {

    public static final IdentityTypeSource STAFF_IDENTITY_TYPE_SOURCE = IdentityTypeSource.newBuilder()
            .setId("staff")
            .addAllAllowedTvmIds(Set.of(1, 2, 3))
            .build();

    public static final IdentityTypeSource ABC_IDENTITY_TYPE_SOURCE = IdentityTypeSource.newBuilder()
            .setId("abc")
            .build();

    private IdentityTypeSourceTestData() {
    }

}
