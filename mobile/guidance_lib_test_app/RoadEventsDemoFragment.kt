package ru.yandex.yandexnavi.guidance_lib_test_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.DialogFragment
import com.yandex.mapkit.road_events.EventTag
import com.yandex.navikit.ui.PlatformImageProvider
import com.yandex.navikit.ui.RoadEventIcon
import ru.yandex.yandexnavi.ui.PlatformImageProviderImpl
import ru.yandex.yandexnavi.ui.util.extensions.*

class RoadEventsDemoFragment : DialogFragment() {

    private lateinit var imageProvider: PlatformImageProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_road_events_demo, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageProvider = PlatformImageProviderImpl(requireContext())
        setupViews(view.findViewById<LinearLayoutCompat>(R.id.root2), true, 2f)
        setupViews(view.findViewById<LinearLayoutCompat>(R.id.root3), true, 1f)
        setupViews(view.findViewById<LinearLayoutCompat>(R.id.root5), false, 2f)
        setupViews(view.findViewById<LinearLayoutCompat>(R.id.root6), false, 1f)
    }

    private fun setupViews(root: LinearLayoutCompat, isSelected: Boolean, scale: Float) {
        val tags = listOf(
            EventTag.LANE_CONTROL,
            EventTag.SPEED_CONTROL,
            EventTag.CROSS_ROAD_CONTROL,
            EventTag.POLICE,
            EventTag.NO_STOPPING_CONTROL
        )

        val addImage = { tagsList: List<EventTag> ->

            val badgeImageName = when {
                tagsList.find { it != EventTag.POLICE && it != EventTag.SPEED_CONTROL } == null -> null
                isSelected -> requireContext().resourceId(R.drawable.poi_alerts_camera_mini_24)
                else -> requireContext().resourceId(R.drawable.poi_alerts_camera_mini_12)
            }

            val image = imageProvider.createRoadEventsImage(
                tagsList.map { tagToIconDescription(it, isSelected) }.toMutableList(),
                badgeImageName,
                isSelected,
                false,
                scale
            )
            val bitmap = image.createImageProvider().image
            val view = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_wrapped_image, root, false).apply {
                    findViewById<AppCompatImageView>(R.id.image).setImageBitmap(bitmap)
                }
            root.addView(view)
        }

        for (firstTag in tags) {
            for (secondTag in tags) {
                if (hashSetOf(firstTag, secondTag).size == 2) {
                    addImage(listOf(firstTag, secondTag))
                }
            }
        }
    }

    private fun tagToIconDescription(tag: EventTag, isSelected: Boolean): RoadEventIcon {
        if (isSelected) {
            return when (tag) {
                EventTag.POLICE -> RoadEventIcon(false, requireContext().resourceId(R.drawable.pin_alerts_camera_40))
                EventTag.LANE_CONTROL -> RoadEventIcon(true, requireContext().resourceId(R.drawable.pin_alerts_lanecamera_40))
                EventTag.CROSS_ROAD_CONTROL -> RoadEventIcon(true, requireContext().resourceId(R.drawable.pin_alerts_crossroad_camera_40))
                EventTag.SPEED_CONTROL -> RoadEventIcon(false, requireContext().resourceId(R.drawable.pin_alerts_camera_40))
                EventTag.NO_STOPPING_CONTROL -> RoadEventIcon(false, requireContext().resourceId(R.drawable.pin_alerts_camera_stop_40))
                else -> throw IllegalStateException("Not supported")
            }
        }
        return when (tag) {
            EventTag.POLICE -> RoadEventIcon(false, requireContext().resourceId(R.drawable.poi_alerts_camera_16))
            EventTag.LANE_CONTROL -> RoadEventIcon(true, requireContext().resourceId(R.drawable.poi_alerts_camera_line_16))
            EventTag.CROSS_ROAD_CONTROL -> RoadEventIcon(true, requireContext().resourceId(R.drawable.poi_alerts_crossroad_camera_16))
            EventTag.SPEED_CONTROL -> RoadEventIcon(false, requireContext().resourceId(R.drawable.poi_alerts_camera_16))
            EventTag.NO_STOPPING_CONTROL -> RoadEventIcon(false, requireContext().resourceId(R.drawable.poi_alerts_camera_stop_16))
            else -> throw IllegalStateException("Not supported")
        }
    }
}
