package ru.yandex.market.di

import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import ru.yandex.market.di.qualifier.DiMain
import io.reactivex.schedulers.Schedulers
import ru.yandex.market.common.schedulers.NetworkingScheduler
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.di.qualifier.DiLocalSingleThread
import ru.yandex.market.di.qualifier.DiCmsNetworking
import ru.yandex.market.di.qualifier.DiCartItems
import javax.inject.Singleton

@Module
object TestSchedulersModule {

    @Provides
    @DiMain
    fun provideMainScheduler(): Scheduler {
        return Schedulers.trampoline()
    }

    @Provides
    @DiLocalSingleThread
    fun provideLocalSingleThreadScheduler(): Scheduler {
        return Schedulers.trampoline()
    }

    @Provides
    @DiCmsNetworking
    @Singleton
    fun provideCmsNetworkingScheduler(): Scheduler {
        return Schedulers.trampoline()
    }

    @Provides
    @DiCartItems
    @Singleton
    fun provideCartItemScheduler(): Scheduler {
        return Schedulers.trampoline()
    }

    @Provides
    fun provideWorkerScheduler(): WorkerScheduler {
        return WorkerScheduler(Schedulers.trampoline())
    }

    @Provides
    fun provideNetworkingScheduler(): NetworkingScheduler {
        return NetworkingScheduler(Schedulers.trampoline())
    }
}