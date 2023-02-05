package com.yandex.mail.shadows

import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.huawei.hms.support.api.client.Status
import org.robolectric.annotation.Implements

@SuppressWarnings("unused")
@Implements(HmsInstanceId::class)
class ShadowHmsInstanceId {

    @Throws(ApiException::class)
    fun getToken(appId: String, scope: String): String? {
        throw ApiException(Status.FAILURE)
    }
}
