/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.api.retrofit;

import com.yandex.datasync.Config;
import com.yandex.datasync.Credentials;
import com.yandex.datasync.internal.api.Api;

import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class RetrofitApiCreatorTest {

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final Credentials MOCK_CREDENTIALS = new Credentials(MOCK_USER_ID, MOCK_TOKEN);

    @Test
    public void testNotNullCredentials() {
        final Config config = new Config.Builder()
                .credentials(MOCK_CREDENTIALS)
                .build();

        final RetrofitApiCreator apiCreator = new RetrofitApiCreator(config);

        final Api api = apiCreator.create();

        assertNotNull(api);
        assertThat(api, instanceOf(RetrofitApi.class));
    }
}