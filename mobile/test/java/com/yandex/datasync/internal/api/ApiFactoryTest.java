/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.api;

import com.yandex.datasync.Config;
import com.yandex.datasync.Credentials;
import com.yandex.datasync.internal.api.retrofit.RetrofitApi;

import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ApiFactoryTest {

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    @Test
    public void testNotNullParams() {
        final Config config = new Config.Builder()
                .credentials(new Credentials(MOCK_USER_ID, MOCK_TOKEN))
                .build();

        final Api api = ApiFactory.create(config);
        assertNotNull(api);
        assertThat(api, instanceOf(RetrofitApi.class));
    }
}