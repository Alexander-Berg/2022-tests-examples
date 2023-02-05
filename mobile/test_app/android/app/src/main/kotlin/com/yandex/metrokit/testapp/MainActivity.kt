package com.yandex.metrokit.testapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.viewpager.widget.ViewPager
import com.yandex.metrokit.Animation
import com.yandex.metrokit.L10nManager
import com.yandex.metrokit.Progress
import com.yandex.metrokit.geometry.Point
import com.yandex.metrokit.scheme.SchemeId
import com.yandex.metrokit.scheme.SchemeSummary
import com.yandex.metrokit.scheme.data.routing.Router
import com.yandex.metrokit.scheme.data.routing.RoutingRequest
import com.yandex.metrokit.scheme_manager.CountryGroup
import com.yandex.metrokit.scheme_manager.Error
import com.yandex.metrokit.scheme_manager.Scheme
import com.yandex.metrokit.scheme_manager.SchemeList
import com.yandex.metrokit.scheme_manager.SchemeListObtainmentSessionListener
import com.yandex.metrokit.scheme_manager.SchemeListUpdatingSessionListener
import com.yandex.metrokit.scheme_manager.SchemeManager
import com.yandex.metrokit.scheme_manager.SchemeObtainmentSessionListener
import com.yandex.metrokit.scheme_manager.SchemeUpdatingSessionListener
import com.yandex.metrokit.scheme_window.camera.Camera
import com.yandex.metrokit.scheme_window.camera.CameraController
import com.yandex.metrokit.testapp.controller.FpsController
import com.yandex.metrokit.testapp.controller.ProgressController
import com.yandex.metrokit.testapp.controller.SchemeViewController
import com.yandex.metrokit.testapp.controller.ScreenPlacemarkData
import com.yandex.metrokit.testapp.controller.StationsSelectListener
import com.yandex.metrokit.view.SchemeView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.ceil
import kotlin.math.floor

@ExperimentalCoroutinesApi
abstract class ScopedAppActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}

@ExperimentalCoroutinesApi
class MainActivity :
        ScopedAppActivity(),
        StationsSelectListener,
        SchemeSummaryAdapter.ItemClickListener {
    private companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var fps: TextView
    private lateinit var libVersion: TextView
    private lateinit var routesView: ViewPager
    private lateinit var schemeView: SchemeView
    private lateinit var updateSchemeProgress: ProgressBar
    private lateinit var commonProgress: ProgressBar

    private val schemeManager: SchemeManager = App.metroKit.createSchemeManager()
    private lateinit var schemeController: SchemeViewController
    private lateinit var updateSchemeProgressController: ProgressController
    private lateinit var commonProgressController: ProgressController
    private lateinit var fpsController: FpsController

    private lateinit var schemeListAdapter: SchemeSummaryAdapter

    private var router: Router? = null

    private var isClosedStationsShown: Boolean = false
    private var currentScheme: Scheme? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fps = findViewById(R.id.fps)
        libVersion = findViewById(R.id.lib_version)
        routesView = findViewById(R.id.routes)
        schemeView = findViewById(R.id.scheme)
        updateSchemeProgress = findViewById(R.id.update_scheme_progress)
        commonProgress = findViewById(R.id.common_progress)

        libVersion.text = getString(R.string.lib_version, App.metroKit.version)

        val pinAData = ScreenPlacemarkData(
                ImageProvider.fromResource(this, R.drawable.pin_a),
                Point(0.5f, 1.0f),
                1.0f
        )
        val pinBData = ScreenPlacemarkData(
                ImageProvider.fromResource(this, R.drawable.pin_b),
                Point(0.5f, 1.0f),
                1.0f
        )

        schemeController = SchemeViewController(
                schemeView.delegate(),
                pinAData,
                pinBData,
                stationsSelectListener = this
        )

        updateSchemeProgress.visibility = View.INVISIBLE
        updateSchemeProgressController = ProgressController(updateSchemeProgress)

        commonProgress.visibility = View.INVISIBLE
        commonProgressController = ProgressController(commonProgress)

        fpsController = FpsController(schemeController::fps, fps)

        schemeListAdapter = SchemeSummaryAdapter(LayoutInflater.from(this), this)

        routesView.visibility = View.INVISIBLE
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.routes_list_horizontal_padding)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.routes_list_vertical_padding)
        routesView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        routesView.pageMargin = resources.getDimensionPixelSize(R.dimen.routes_page_margin)
        routesView.clipToPadding = false

        setSupportActionBar(findViewById(R.id.toolbar))

        isClosedStationsShown = false

        launch { initializeSchemeView() }
    }

    override fun onStart() {
        super.onStart()
        schemeView.onStart()
    }

    override fun onStop() {
        schemeView.onStop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        fpsController.start()
    }

    override fun onPause() {
        fpsController.stop()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val currentScheme = currentScheme
        if (currentScheme != null) {
            setupMenuSpinner(
                    R.id.lang,
                    menu,
                    currentScheme.languages,
                    currentScheme.defaultLanguage,
                    schemeController::setLanguage,
                    { it.value }
            )
            setupMenuSpinner(
                    R.id.style,
                    menu,
                    currentScheme.styles,
                    currentScheme.defaultStyle,
                    schemeController::setStyle
            )
        }

        return true
    }

    private fun <T> setupMenuSpinner(
            @IdRes resId: Int,
            menu: Menu,
            values: List<T>,
            defaultValue: T,
            onSelect: (value: T) -> Unit,
            toString: (value: T) -> String = { it.toString() }
    ) {
        val spinner = menu.findItem(resId).actionView as Spinner
        spinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                values.map(toString)
        )
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSelect(values[position])
            }
        }
        spinner.setSelection(values.indexOf(defaultValue))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.update_scheme_list -> {
                launch {
                    updateSchemeListAsync().await()
                }
            }
            R.id.choose_scheme -> {
                AlertDialog.Builder(this@MainActivity)
                        .setSingleChoiceItems(
                                schemeListAdapter,
                                -1
                        ) { dialog, which ->
                            launch { onSchemeSelected(schemeListAdapter.data[which].schemeId) }
                            dialog.dismiss()
                        }
                        .show()
            }
            R.id.set_zoom -> {
                showZoomPicker()
            }
            R.id.export_pdf -> {
                val schemeName = currentScheme?.summary?.name
                        ?.let(L10nManager::getDefaultValue)
                        ?: return true
                val pdfFile = File(cacheDir, "export/$schemeName.pdf").apply {
                    delete()
                    parentFile.mkdirs()
                }
                schemeView.delegate().window.renderToPdf(pdfFile.absolutePath)
                if (pdfFile.exists() && pdfFile.isFile) {
                    viewPdfFile(pdfFile)
                } else {
                    Toast.makeText(this, R.string.export_pdf_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    private fun showZoomPicker() {
        val cameraController: CameraController = schemeView.delegate().window.cameraController
        val data = getNumberPickerDisplayValues(cameraController)
        val dialog = AlertDialog.Builder(this@MainActivity)
                .setView(R.layout.zoom_picker)
                .show()
        val numberPicker: NumberPicker = dialog.findViewById(R.id.number_picker)!!
        numberPicker.apply {
            displayedValues = data.map { it.second }.toTypedArray()
            minValue = 0
            maxValue = data.lastIndex
        }
        dialog.findViewById<View>(R.id.ok)!!.apply {
            setOnClickListener {
                val dataIndex = numberPicker.value
                val scale = data[dataIndex].first
                val prevCamera = cameraController.camera
                cameraController.setCamera(
                        Camera(scale, prevCamera.position),
                        Animation(Animation.Type.CUBIC_EASE_IN_OUT, 0.3f)
                )
                dialog.dismiss()
            }
        }
        dialog.findViewById<View>(R.id.cancel)!!.apply {
            setOnClickListener { dialog.dismiss() }
        }
    }

    private fun getNumberPickerDisplayValues(
            cameraController: CameraController
    ): List<Pair<Float, String>> {
        val minScale = cameraController.minScale
        val maxScale = cameraController.maxScale

        if (maxScale < minScale) {
            return emptyList()
        }

        val result = mutableListOf<Pair<Float, String>>()

        if (floor(minScale) != minScale) {
            result += minScale to "%.2f".format(minScale)
        }

        for (value in ceil(minScale).toInt()..floor(maxScale).toInt()) {
            result += value.toFloat() to "%d".format(value)
        }

        if (ceil(maxScale) != maxScale) {
            result += maxScale to "%.2f".format(maxScale)
        }

        return result
    }

    private fun viewPdfFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "com.yandex.metrokit.testapp.fileprovider", file)
        val viewIntent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(viewIntent)
    }

    override fun onStationsSelect(stationA: String, stationB: String) {
        val routes = router?.buildRoute(RoutingRequest(stationA, stationB))?.main
        if (routes != null && routes.isNotEmpty()) {
            routesView.adapter = RouteAdapter(this, routes)
            routesView.clearOnPageChangeListeners()
            routesView.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
                override fun onPageSelected(position: Int) {
                    schemeController.showRoute(routes[position])
                }
            })
            routesView.visibility = View.VISIBLE

            schemeController.showRoute(routes[0])
        } else {
            onStationSelectClear()
        }
    }

    override fun onStationSelectClear() {
        routesView.visibility = View.INVISIBLE
        schemeController.dismissRoute()
    }

    override fun onUpdateClick(position: Int, schemeSummary: SchemeSummary) {
        launch {
            updateSchemeAsync(schemeSummary.schemeId).await()
            updateSchemeListAsync().await()
        }
    }

    private suspend fun onSchemeSelected(schemeId: SchemeId) {
        onStationSelectClear()
        loadScheme(schemeId)
    }

    private suspend fun loadScheme(schemeId: SchemeId) {
        val scheme = getSchemeAsync(schemeId).await()

        schemeController.setScheme(scheme)
        router = scheme.makeRouter()
        currentScheme = scheme
        invalidateOptionsMenu()
    }

    private suspend fun initializeSchemeView() {
        updateSchemeListAdapter()

        val schemeList = getSchemeListAsync().await()
        val initialScheme = schemeList.schemeIdByAlias("moscow")

        if (initialScheme != null) {
            loadScheme(initialScheme)
        }
    }

    private suspend fun updateSchemeListAdapter() {
        val schemeList = getSchemeListAsync().await()
        schemeListAdapter.data = schemeList.items.allSchemes().toMutableList()
        schemeListAdapter.notifyDataChanged()
    }

    private fun CoroutineScope.updateSchemeListAsync() = async(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { c ->
            commonProgressController.show()
            val listener = object : SchemeListUpdatingSessionListener {
                override fun onSchemeListUpdateResult() {
                    c.resume(Unit)
                    launch { updateSchemeListAdapter() }
                    commonProgressController.hide()
                }

                override fun onSchemeListUpdateError(error: Error) {
                    logError(error)
                    commonProgressController.hide()
                }
            }
            val session = schemeManager.updateSchemeList()
            session.addListener(listener)
            c.invokeOnCancellation {
                if (session.isValid) {
                    session.removeListener(listener)
                    session.cancel()
                }
            }
        }
    }

    private fun CoroutineScope.getSchemeListAsync() = async(Dispatchers.Main) {
        suspendCancellableCoroutine<SchemeList> { c ->
            commonProgressController.show()
            val listener = object : SchemeListObtainmentSessionListener {
                override fun onSchemeListObtainmentResult(schemeList: SchemeList) {
                    c.resume(schemeList)
                    commonProgressController.hide()
                }

                override fun onSchemeListObtainmentError(error: Error) {
                    logError(error)
                    commonProgressController.hide()
                }
            }
            val session = schemeManager.schemeList(false)
            session.addListener(listener)
            c.invokeOnCancellation {
                if (session.isValid) {
                    session.removeListener(listener)
                    session.cancel()
                }
            }
        }
    }

    private fun CoroutineScope.updateSchemeAsync(schemeId: SchemeId) = async(Dispatchers.Main) {
        suspendCancellableCoroutine<SchemeSummary> { c ->
            updateSchemeProgressController.show()
            val listener = object : SchemeUpdatingSessionListener {
                override fun onSchemeUpdateResult(summary: SchemeSummary) {
                    c.resume(summary)
                    updateSchemeProgressController.hide()
                }

                override fun onSchemeUpdateProgress(progress: Progress) {
                    updateSchemeProgressController.setProgress(progress.value)
                }

                override fun onSchemeUpdateError(error: Error) {
                    logError(error)
                    updateSchemeProgressController.hide()
                }
            }
            val session = schemeManager.updateScheme(schemeId)
            session.addListener(listener)
            c.invokeOnCancellation {
                if (session.isValid) {
                    session.removeListener(listener)
                    session.cancel()
                }
            }
        }
    }

    private fun CoroutineScope.getSchemeAsync(schemeId: SchemeId) = async(Dispatchers.Main) {
        suspendCancellableCoroutine<Scheme> { c ->
            val listener = object : SchemeObtainmentSessionListener {
                override fun onSchemeObtainmentResult(scheme: Scheme) {
                    c.resume(scheme)
                    commonProgressController.hide()
                }

                override fun onSchemeObtainmentError(error: Error) {
                    logError(error)
                    commonProgressController.hide()
                }
            }
            val session = schemeManager.scheme(schemeId)
            session.addListener(listener)
            c.invokeOnCancellation {
                if (session.isValid) {
                    session.removeListener(listener)
                    session.cancel()
                }
            }
        }
    }

    private fun Iterable<CountryGroup>.allSchemes(): Iterable<SchemeSummary> {
        return flatMap { it.schemes }
    }

    private fun SchemeList.schemeIdByAlias(alias: String): SchemeId? {
        return items.allSchemes().find {
            alias in it.aliases
        }?.schemeId
    }

    private fun logError(error: Error) {
        Log.e(TAG, error.message)
        Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
    }
}
