package ru.yandex.market.uikitapp

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class TestActivityModule {

    @ContributesAndroidInjector(modules = [ParentFragmentModule::class])
    abstract fun contributesParentFragment(): ParentFragment
}