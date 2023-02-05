package ru.yandex.yandexmaps.commons.utils.parcel

import android.graphics.Color
import android.os.Parcel
import com.yandex.mapkit.LocalizedValue
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.Line
import com.yandex.mapkit.search.Stop
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.yandexmaps.common.mapkit.bundlers.createSerializable
import ru.yandex.yandexmaps.common.mapkit.bundlers.putSerializable
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ParcelUtilsTest {

    @Test
    fun stopParcelization() {
        val name = "stop_name"
        val distance = LocalizedValue(5.0, "five meters")
        val style = Stop.Style(Color.RED)
        val point = Point(45.0, 33.0)

        val oldStop = Stop(name, distance, style, point, "stop_id", Line("line"))

        val parcel = Parcel.obtain()
        parcel.putSerializable(oldStop)
        parcel.setDataPosition(0)

        val newStop = parcel.createSerializable<Stop>()
        parcel.recycle()

        assertEquals(name, newStop.name)
        assertEquals(distance.text, newStop.distance.text)
        assertEquals(distance.value, newStop.distance.value)
        assertEquals(style.color, newStop.style.color)
        assertEquals(point.latitude, newStop.point.latitude)
        assertEquals(point.longitude, newStop.point.longitude)
    }
}
