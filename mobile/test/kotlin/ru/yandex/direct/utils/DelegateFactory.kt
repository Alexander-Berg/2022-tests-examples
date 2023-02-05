// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.utils

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.mock.BehaviorDelegate
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior
import java.util.concurrent.TimeUnit

class DelegateFactory {
    companion object {
        fun <T> create(service: Class<T>): BehaviorDelegate<T> {
            val retrofit = Retrofit.Builder().baseUrl("https://ya.ru")
                    .client(OkHttpClient())
                    .build()

            val behavior = NetworkBehavior.create().apply {
                setDelay(0, TimeUnit.MILLISECONDS)
                setErrorPercent(0)
                setFailurePercent(0)
                setVariancePercent(0)
            }

            return MockRetrofit.Builder(retrofit)
                    .networkBehavior(behavior)
                    .build()
                    .create(service)
        }
    }
}