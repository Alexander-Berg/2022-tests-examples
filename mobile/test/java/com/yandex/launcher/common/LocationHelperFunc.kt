package com.yandex.launcher.common

import android.location.Location
import com.yandex.launcher.common.location.LocationProvider
import org.hamcrest.core.Is
import org.hamcrest.core.IsNull
import org.junit.Assume
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.atomic.AtomicReference

fun LocationProvider.initLocationProviderWithLocation(location: Location?) {
    Assume.assumeThat(this.getOldLocationFromProvider(), IsNull.nullValue())
    ReflectionHelpers.setField(this, "locationRef", AtomicReference<Location>(location))
    Assume.assumeThat(this.getOldLocationFromProvider(), Is.`is`(location))
}

fun LocationProvider.getOldLocationFromProvider() = ReflectionHelpers.getField<AtomicReference<Location>>(this, "locationRef")?.get()