package com.yandex.maps.testapp.mapdesign

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Consumer
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import com.google.android.material.slider.Slider
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.StyleType
import com.yandex.mapkit.ZoomRange
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.geo.Projections
import com.yandex.mapkit.glyphs.GlyphUrlProvider
import com.yandex.mapkit.images.ImageUrlProvider
import com.yandex.mapkit.layers.*
import com.yandex.mapkit.logo.Alignment
import com.yandex.mapkit.logo.HorizontalAlignment
import com.yandex.mapkit.logo.VerticalAlignment
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.resource_url_provider.ResourceUrlProvider
import com.yandex.mapkit.tiles.UrlProvider
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.maps.testapp.*
import com.yandex.maps.testapp.experiments.Experiment
import com.yandex.maps.testapp.experiments.ExperimentsUtils
import com.yandex.maps.testapp.map.MapCustomizationDialog
import com.yandex.maps.testapp.mapdesign.Utils.requestContent
import com.yandex.runtime.bindings.Serialization
import com.yandex.runtime.view.GraphicsAPIType
import org.json.JSONArray
import org.json.JSONException
import java.util.*
import java.util.logging.Logger
import kotlin.concurrent.fixedRateTimer
import kotlin.math.abs


class MapDesignActivity : TestAppActivity(), InputListener, GeoObjectTapListener {
    private companion object {
        val LOGGER = Logger.getLogger("yandex.MapDesign")
        const val UPDATE_DELAY = 1500L  // ms
        const val TILE_APPEARING_ANIMATION_DURATION = 100L  // ms
        const val INTENT_CAMERA_POSITION = "intent_camera_position"
        const val INTENT_PROD_MODE_ENABLED = "intent_prod_mode_enabled"
        const val INTENT_USE_LATEST_SESSION_ID = "intent_use_latest_session_id"
        const val INTENT_LOCALE = "intent_locale"
        const val INTENT_FOV = "intent_fov"
        const val INTENT_TILT_LIMIT = "intent_tilt_limit"
        const val USER_NAME = "user_name"
        const val SESSION_ID = "session_id"
        const val NIGHT_MODE = "night_mode"
        const val ENABLED = "enabled"
        const val DISABLED = ""
        val MOSCOW = CameraPosition(Point(55.755793, 37.617134), 9.0f, 0.0f, 0.0f)
    }

    enum class StyleTarget { BASEMAP }

    private enum class SupportedTileFormat(val styleType: StyleType) {
        VEC2(StyleType.V_MAP2),
        VEC3(StyleType.V_MAP3),
    }

    data class SessionRecord(val id: String, val styleKey: String, val updatedAt: String) {
        override fun toString() = id
    }

    data class BranchButtonsVisibility(val service: Boolean, val branch: Boolean, val styleSet: Boolean)

    @Volatile var lastFoundUserSessionIds = ArrayList<SessionRecord>()
    @Volatile var lastFoundUsers = setOf<String>()
    @Volatile var branches = arrayOf<BranchHead>()
    @Volatile var services = arrayOf<Service>()
    private var skipNextUpdate = false
    private var version: Long = 0
    private var maxTiltLimit = 60f
    private var tileFormat = SupportedTileFormat.VEC2
    private var lastSession = SessionRecord("", "", "0")
    private var styledLayer: Layer? = null
    private var tileUrlProvider: UrlProvider? = null
    private var vec3StyleUrlProvider: ResourceUrlProvider? = null
    private val projection = Projections.getWgs84Mercator()
    private var useLatestSessionId = true
    private var branchMode = false
    private var prodModeEnabled = false
    private var errorDialogIsShown = false

    private lateinit var iconUrlProvider: ImageUrlProvider
    private lateinit var modelUrlProvider: ResourceUrlProvider
    private lateinit var glyphUrlProvider: GlyphUrlProvider

    private lateinit var host: Host
    private val storage = PermanentStorage()
    private var currentService: Service? = null
    private var currentBranchHead: BranchHead? = null
    private var currentStyleSetName: String? = null
    private val layerOptions = LayerOptions()
            .setAnimateOnActivation(true)
            .setTileAppearingAnimationDuration(TILE_APPEARING_ANIMATION_DURATION)
            .setOverzoomMode(OverzoomMode.WITH_PREFETCH)
            .setTransparent(true)

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sessionLayout: LinearLayout
    private lateinit var branchLayout: LinearLayout
    private lateinit var customizationLayout: LinearLayout
    private lateinit var sessionIdSelectorButton: Button
    private lateinit var userSelectorButton: Button
    private lateinit var styleSetNameSelectorButton: Button
    private lateinit var branchSelectorButton: Button
    private lateinit var serviceSelectorButton: Button
    private lateinit var mapModeSelectorButton: Button
    private lateinit var useStyleButton: ToggleButton
    private lateinit var localeEditorButton: Button
    private lateinit var zoomView: TextView
    private lateinit var enableUserLocationButton: ToggleButton
    private lateinit var followUserLocationButton: ToggleButton
    private lateinit var rotateUserLocationButton: ToggleButton
    private lateinit var zoomUserLocationButton: ToggleButton
    private lateinit var directionButton: ImageButton

    private lateinit var mapview: MapView
    private lateinit var userLocationLayer : UserLocationLayer
    private lateinit var updateHandler: Timer
    private lateinit var customizationDialog: CustomizationDialog
    private var locale: String
        get() = localeEditorButton.text.toString()
        set(value) {
            localeEditorButton.text = value
        }

    private val onLocaleApply = Consumer<String>{ resetLocale(it) }
    private val cameraListener = CameraListener { _, position, _, _ -> updateZoomView(position.zoom); updateDirectionView(position.azimuth) }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (vulkanPreferred())
            setTheme(R.style.VulkanTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_design)

        host = Host(context = this)
        useStyleButton = findViewById(R.id.use_style_switch)
        customizationLayout = findViewById(R.id.customization_layout)
        localeEditorButton = findViewById(R.id.locale_editor)
        zoomView = findViewById(R.id.zoom_view)
        mapview = findViewById(R.id.mapview)
        setMapType(MapType.NONE)
        mapview.map.apply {
            addYandexLayerId("DesignMapLayer")
            addInputListener(this@MapDesignActivity)
            addTapListener(this@MapDesignActivity)
        }

        if (vulkanPreferred() && !vulkanEnabled()) {
            showToast("Vulkan is not supported by this device")
            VulkanTools.storeVulkanPreferred(applicationContext, false)
        }

        initTileFormat()
        initDrawer()
        setSessionId(storage.load(SESSION_ID))
        initCameraPosition()
        initLocale()
        initProdMode()
        createProviders(host.config.rendererHost)

        mapview.fieldOfViewY = intent.getDoubleExtra(INTENT_FOV, mapview.fieldOfViewY)
        updateTiltExperiment(intent.getFloatExtra(INTENT_TILT_LIMIT, maxTiltLimit))

        directionButton = findViewById(R.id.direction)
        mapview.map.addCameraListener(cameraListener)
        updateZoomView(mapview.map.cameraPosition.zoom)
        updateDirectionView(mapview.map.cameraPosition.azimuth)

        customizationDialog = CustomizationDialog()

        mapview.map.logo.setAlignment(Alignment(HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM))

        createUserLocationLayer()
    }

    @UiThread
    private fun createUserLocationLayer() {
        enableUserLocationButton = findViewById(R.id.enable_user_location_switch)
        followUserLocationButton = findViewById(R.id.follow_user_location_switch)
        rotateUserLocationButton = findViewById(R.id.rotate_map_user_location_switch)
        zoomUserLocationButton = findViewById(R.id.auto_zoom_user_location_switch)

        userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapview.mapWindow)
        userLocationLayer.isVisible = false
    }

    @UiThread
    private fun initProdMode() {
        if (intent.getBooleanExtra(INTENT_PROD_MODE_ENABLED, prodModeEnabled))
            findViewById<ToggleButton>(R.id.prod_switch).performClick()
    }

    @UiThread
    private fun updateTiltExperiment(maxTiltLimit: Float) {
        if (this.maxTiltLimit != maxTiltLimit) {
            this.maxTiltLimit = maxTiltLimit
            val experiment = Experiment("MAPKIT", "max_tilt_limit", maxTiltLimit.toString())
            ExperimentsUtils.refreshCustomExperiment(experiment)
        }
    }

    @UiThread
    private fun initLocale() {
        locale = resources.getString(R.string.ru_locale)
        intent.getStringExtra(INTENT_LOCALE)?.let { locale = it }
    }

    @UiThread
    private fun initCameraPosition() {
        var position = MOSCOW
        val intentPosition = intent.getByteArrayExtra(INTENT_CAMERA_POSITION)
        intentPosition?.let { position = Serialization.deserializeFromBytes(it, CameraPosition::class.java) }
        mapview.map.move(position)
    }

    @UiThread
    private fun updateZoomView(zoom: Float) {
        val newZoom = "Zoom: %.2f".format(zoom)
        if (zoomView.text != newZoom)
            zoomView.text = newZoom
    }

    @UiThread
    private fun updateDirectionView(azimuth: Float) {
        if (abs(directionButton.rotation - azimuth) >= 1)
            directionButton.rotation = -azimuth
    }

    @UiThread
    private fun setMapType(mapType: MapType) {
        mapview.map.mapType = mapType
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(R.layout.map_design_drawer)
        val contentView: ViewGroup  = findViewById(R.id.content_frame)
        View.inflate(this, layoutResID, contentView)
        drawerLayout = findViewById(R.id.drawer_layout)
    }

    @UiThread
    private fun createProviders(rendererHost: String) {
        iconUrlProvider = ImageUrlProvider {
            desc -> "https://$rendererHost/icons?id=${desc.imageId}"
        }
        modelUrlProvider = ResourceUrlProvider { "" }
        glyphUrlProvider = GlyphUrlProvider { fontId, range ->
            "https://$rendererHost/glyphs" +
                    "?font_id=$fontId" +
                    "&range=${range.firstGlyphId},${range.lastGlyphId}"
        }
    }

    @UiThread
    override fun onStartImpl() {
        mapview.onStart()
        updateHandler = fixedRateTimer(name = "map-design-updater", period = UPDATE_DELAY, daemon = true) {
            update()
        }
    }

    @UiThread
    override fun onStopImpl() {
        updateHandler.cancel()
        mapview.onStop()
    }

    @UiThread
    private fun initTileFormat() {
        val currentTileFormat = MapkitAdapter.getMapStyleType(applicationContext)
        val tileFormatExtraName = "tile_format"
        if (intent.hasExtra(tileFormatExtraName)) {
            tileFormat = when (val tileFormatExtra = intent.getStringExtra(tileFormatExtraName)) {
                "vec2" -> SupportedTileFormat.VEC2
                "vec3" -> SupportedTileFormat.VEC3
                else   -> throw RuntimeException("Unknown ${tileFormatExtraName}: $tileFormatExtra")
            }
            if (tileFormat.styleType != currentTileFormat)
                applyTileFormat()
        } else if (tileFormat.styleType != currentTileFormat) {
            when (currentTileFormat) {
                StyleType.V_MAP2 -> tileFormat = SupportedTileFormat.VEC2
                StyleType.V_MAP3 -> tileFormat = SupportedTileFormat.VEC3
                else -> { tileFormat = SupportedTileFormat.VEC2; applyTileFormat() }
            }
        }
    }

    @UiThread
    private fun applyTileFormat() {
        MapkitAdapter.setMapStyleType(applicationContext, tileFormat.styleType)
        restartApp()
    }

    @UiThread
    private fun vulkanPreferred(): Boolean {
        if (intent.hasExtra(VulkanTools.VULKAN_PREFERRED_KEY)) {
            val vulkanPreferredByIntent = intent.getBooleanExtra(VulkanTools.VULKAN_PREFERRED_KEY, false)
            VulkanTools.storeVulkanPreferred(applicationContext, vulkanPreferredByIntent)
        }
        return VulkanTools.readVulkanPreferred(applicationContext)
    }

    @UiThread
    private fun vulkanEnabled() = mapview.graphicsAPI == GraphicsAPIType.VULKAN

    @UiThread
    private fun restartApp() {
        val startCurActivity = Intent(this, this::class.java)
        startCurActivity.putExtra(INTENT_CAMERA_POSITION, Serialization.serializeToBytes(mapview.map.cameraPosition))
        startCurActivity.putExtra(INTENT_PROD_MODE_ENABLED, prodModeEnabled)
        startCurActivity.putExtra(INTENT_USE_LATEST_SESSION_ID, useLatestSessionId)
        startCurActivity.putExtra(INTENT_LOCALE, locale)
        startCurActivity.putExtra(INTENT_FOV, mapview.fieldOfViewY)
        startCurActivity.putExtra(INTENT_TILT_LIMIT, maxTiltLimit)


        startCurActivity.addFlags(FLAG_ACTIVITY_NEW_TASK)
        startActivity(startCurActivity)

        finish()
        Runtime.getRuntime().exit(0)
    }

    @UiThread
    fun onChangeVulkanClicked(view: View) {
        if ((view.id == R.id.vulkan_on) != vulkanEnabled()) {
            VulkanTools.storeVulkanPreferred(applicationContext, !vulkanEnabled())
            restartApp()
        }
    }

    @UiThread
    fun onChangeVecClicked(view: View) {
        val newTileFormat = if (view.id == R.id.vec3) SupportedTileFormat.VEC3 else SupportedTileFormat.VEC2
        if (newTileFormat != tileFormat) {
            tileFormat = newTileFormat
            applyTileFormat()
        }
    }

    @UiThread
    fun onChangeNightModeClicked(view: View) {
        val night = (view as ToggleButton).isChecked
        mapview.map.isNightModeEnabled = night
        storage.save(NIGHT_MODE, if (night) ENABLED else DISABLED)
    }

    @UiThread
    fun onEnableUserLocationClicked(view: View) {
        userLocationLayer.isVisible = (view as ToggleButton).isChecked
        updateUserLocationButtonsVisibility()
    }

    @UiThread
    fun onFollowUserLocationClicked(view: View) {
        if ((view as ToggleButton).isChecked)
            userLocationLayer.setAnchor(getAnchorCenter(), getAnchorCourse())
        else
            userLocationLayer.resetAnchor()
        updateUserLocationButtonsVisibility()
    }

    @UiThread
    fun onRotateMapUserLocationClicked(view: View) {
        userLocationLayer.isHeadingEnabled = (view as ToggleButton).isChecked
    }

    @UiThread
    fun onAutoZoomUserLocationClicked(view: View) {
        userLocationLayer.isAutoZoomEnabled = (view as ToggleButton).isChecked
    }

    @UiThread
    fun onOptionsClicked(view: View) = drawerLayout.openDrawer(GravityCompat.START)

    @UiThread
    fun onDirectionClicked(view: View) {
        val from = mapview.map.cameraPosition
        val to = CameraPosition(from.target, from.zoom, 0.0f, from.tilt)
        mapview.map.move(to, Animation(Animation.Type.SMOOTH, 0.2f)) {}
    }

    @UiThread
    fun updateUserLocationButtonsVisibility() {
        val isEnabled = enableUserLocationButton.isChecked
        val isFollowing = followUserLocationButton.isChecked
        followUserLocationButton.isEnabled = isEnabled
        rotateUserLocationButton.isEnabled = isEnabled && isFollowing
        zoomUserLocationButton.isEnabled = isEnabled && isFollowing
    }

    @UiThread
    fun showExtraSettings(view: View) {
        drawerLayout.closeDrawer(GravityCompat.START)
        ExtraSettingsDialog().show(supportFragmentManager, "extra_settings_dialog")
    }

    @UiThread
    fun openCustomizationDialog(view: View) = customizationDialog.show()

    @UiThread
    fun openLocaleEditor(view: View) = LocaleDialog.show(this, locale, onLocaleApply)

    @UiThread
    fun onUseStyleClicked(view: View) = updateStyle()

    @UiThread
    fun onProdClicked(view: View) {
        prodModeEnabled = (view as ToggleButton).isChecked
        styledLayer?.activate(!prodModeEnabled)
        setMapType(if (prodModeEnabled) MapType.VECTOR_MAP else MapType.NONE)
        if (!prodModeEnabled && branchMode)
            currentStyleSetName?.let { setMapType(inferMapType(it)) }

        customizationLayout.visibility = if (prodModeEnabled) View.GONE else View.VISIBLE
    }

    private fun getAnchorCenter(): PointF {
        return PointF((mapview.width * 0.5).toFloat(), (mapview.height * 0.5).toFloat())
    }

    private fun getAnchorCourse(): PointF {
        return PointF((mapview.width * 0.5).toFloat(), (mapview.height * 0.83).toFloat())
    }

    @UiThread
    private fun initDrawer() {
        drawerLayout.setScrimColor(Color.TRANSPARENT);
        findViewById<RadioGroup>(R.id.vulkan_switch).check(if (vulkanPreferred()) R.id.vulkan_on else R.id.vulkan_off)

        findViewById<RadioGroup>(R.id.vec_switch).check(
            when (tileFormat) {
                SupportedTileFormat.VEC2 -> R.id.vec2
                SupportedTileFormat.VEC3 -> R.id.vec3
            }
        )

        sessionLayout = findViewById(R.id.session_layout)
        branchLayout = findViewById(R.id.branch_layout)
        findViewById<RadioButton>(R.id.session_mode).performClick()

        useLatestSessionId = intent.getBooleanExtra(INTENT_USE_LATEST_SESSION_ID, useLatestSessionId)
        findViewById<ToggleButton>(R.id.use_latest_session_mode_switch).isChecked = useLatestSessionId
        initUserSelector()
        initSessionIdSelector()
        initServiceSelector()
        initBranchSelector()
        initStyleSetNameSelector()
        initMapModeSelector()
        initNightMode()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putByteArray(INTENT_CAMERA_POSITION, Serialization.serializeToBytes(mapview.map.cameraPosition))
        outState.putDouble(INTENT_FOV, mapview.fieldOfViewY)
        outState.putFloat(INTENT_TILT_LIMIT, maxTiltLimit)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        onChangeSessionModeClicked(findViewById<ToggleButton>(R.id.use_latest_session_mode_switch))
        onChangeSessionOrBranchModeClicked(findViewById(findViewById<RadioGroup>(R.id.session_or_branch_switch).checkedRadioButtonId))
        onProdClicked(findViewById<ToggleButton>(R.id.prod_switch))

        val intentPosition = savedInstanceState.getByteArray(INTENT_CAMERA_POSITION)
        intentPosition?.let { mapview.map.move(Serialization.deserializeFromBytes(it, CameraPosition::class.java)) }
        mapview.fieldOfViewY = savedInstanceState.getDouble(INTENT_FOV, mapview.fieldOfViewY)
        updateTiltExperiment(savedInstanceState.getFloat(INTENT_TILT_LIMIT, maxTiltLimit))

        followUserLocationButton.isChecked = false
        onEnableUserLocationClicked(enableUserLocationButton)
        onFollowUserLocationClicked(followUserLocationButton)
        onRotateMapUserLocationClicked(rotateUserLocationButton)
        onAutoZoomUserLocationClicked(zoomUserLocationButton)
    }

    @UiThread
    private fun initNightMode() {
        if (storage.load(NIGHT_MODE) == ENABLED)
            findViewById<ToggleButton>(R.id.night_mode_switch).performClick()
    }

    @UiThread
    private fun initUserSelector() {
        val userName = storage.load(USER_NAME)
        userSelectorButton = findViewById(R.id.user_selector)
        userSelectorButton.text = userName
        userSelectorButton.setOnClickListener {
            if (lastFoundUsers.isEmpty())
                showToast("No users found")
            else
                UserSelectorDialog().show(supportFragmentManager, "user_dialog")
        }
    }

    @UiThread
    private fun initSessionIdSelector() {
        sessionIdSelectorButton = findViewById(R.id.session_id_selector)
        sessionIdSelectorButton.isEnabled = !useLatestSessionId
        sessionIdSelectorButton.setOnClickListener {
            if (lastFoundUserSessionIds.isEmpty())
                showToast("There are no sessions for the current user")
            else
                SessionIdSelectorDialog().show(supportFragmentManager, "session_id_dialog")
        }
    }

    @UiThread
    private fun initServiceSelector() {
        serviceSelectorButton = findViewById(R.id.service_selector)
        serviceSelectorButton.setOnClickListener {
            val vis = getBranchButtonsVisibility()
            setBranchButtonsVisibility(BranchButtonsVisibility(service = false, branch = false, styleSet = false))
            val text = serviceSelectorButton.text.toString()
            serviceSelectorButton.text = "Loading..."
            Thread {
                withExceptionHandling { services = host.requestServices() }
                runOnUiThread {
                    setBranchButtonsVisibility(vis)
                    serviceSelectorButton.text = text
                    if (services.isEmpty())
                        showToast("No services found")
                    else
                        ServiceSelectorDialog().show(supportFragmentManager, "service_dialog")
                }
            }.start()
        }
    }

    @UiThread
    private fun initBranchSelector() {
        branchSelectorButton = findViewById(R.id.branch_selector)
        branchSelectorButton.setOnClickListener {
            val vis = getBranchButtonsVisibility()
            setBranchButtonsVisibility(BranchButtonsVisibility(service = false, branch = false, styleSet = false))
            val text = branchSelectorButton.text.toString()
            branchSelectorButton.text = "Loading..."
            Thread {
                withExceptionHandling { currentService?.let{ branches = it.requestBranchHeads() } }
                runOnUiThread {
                    setBranchButtonsVisibility(vis)
                    branchSelectorButton.text = text
                    if (branches.isEmpty())
                        showToast("No branches found")
                    else
                        BranchSelectorDialog().show(supportFragmentManager, "branch_dialog")
                }
            }.start()
        }
        branchSelectorButton.isEnabled = false
    }

    @UiThread
    private fun initStyleSetNameSelector() {
        styleSetNameSelectorButton = findViewById(R.id.style_set_selector)
        styleSetNameSelectorButton.setOnClickListener {
            getStyleSetNames()?.let{
                if (it.isEmpty())
                    showToast("No style set names found")
                else
                    StyleSetNameSelectorDialog().show(supportFragmentManager, "style_set_name_dialog")
            }
        }
        styleSetNameSelectorButton.isEnabled = false
    }

    @UiThread
    private fun initMapModeSelector() {
        mapModeSelectorButton = findViewById(R.id.map_mode_selector)
        mapModeSelectorButton.setOnClickListener {
            MapModeSelectorDialog().show(supportFragmentManager, "map_mode_dialog")
        }
        val mapModeLayout: LinearLayout = findViewById(R.id.map_mode_layout)
        when (tileFormat) {
            SupportedTileFormat.VEC2 -> mapModeLayout.visibility = View.GONE
            SupportedTileFormat.VEC3 -> mapModeLayout.visibility = View.VISIBLE
        }
        setMapMode(MapMode.DEFAULT)
    }

    @UiThread
    fun onChangeSessionModeClicked(view: View) {
        useLatestSessionId = (view as ToggleButton).isChecked
        sessionIdSelectorButton.isEnabled = !useLatestSessionId
    }

    @UiThread
    fun onChangeSessionOrBranchModeClicked(view: View) {
        if (view.id == R.id.branch_mode) {
            sessionLayout.visibility = View.GONE
            branchLayout.visibility = View.VISIBLE
        } else {
            sessionLayout.visibility = View.VISIBLE
            branchLayout.visibility = View.GONE
        }
        branchMode = view.id == R.id.branch_mode
        reloadStyledLayer()
    }

    @UiThread
    private fun resetLocale(newLocale: String) {
        if (locale != newLocale) {
            locale = newLocale
            reloadStyledLayer()
        }
    }

    private fun getUserSessions(): ArrayList<SessionRecord>? {
        return withExceptionHandling {
            val sessions = ArrayList<SessionRecord>()
            val users = mutableSetOf<String>()
            val urlString = "https://${host.config.rendererHost}/session"
            val content =  JSONArray(requestContent(urlString))

            val userName = storage.load(USER_NAME)
            for (i in 0 until(content.length())) {
                val sessionRecord = content.getJSONObject(i)
                if (!sessionRecord.has("style_key") || !sessionRecord.has("id") || !sessionRecord.has("user") || !sessionRecord.has("updated_at"))
                    continue
                users.add(sessionRecord.getString("user"))
                if (sessionRecord.getString("user") == userName)
                    sessions.add(SessionRecord(sessionRecord.getString("id"), sessionRecord.getString("style_key"), sessionRecord.getString("updated_at")))
            }

            sessions.sortWith(Comparator{ x, y -> y.updatedAt.compareTo(x.updatedAt) })
            lastFoundUserSessionIds = sessions
            lastFoundUsers = users

            sessions
        }
    }

    private fun choiceSession(sessions: ArrayList<SessionRecord>): SessionRecord {
        if (useLatestSessionId)
            return sessions[0]
        for (session in sessions) {
            if (session.id == lastSession.id)
                return session
        }
        return sessions[0]
    }

    private fun update() {
        if (skipNextUpdate) {
            skipNextUpdate = false
            return
        }

        if (branchMode || prodModeEnabled)
            return

        val sessions = getUserSessions()
        if (sessions == null)
            return
        if (sessions.isEmpty()) {
            runOnUiThread { setSessionId("") }
            return
        }
        val session = choiceSession(sessions)
        if (session.id == lastSession.id && session.styleKey == lastSession.styleKey)
            return

        runOnUiThread {
            if (!branchMode) {
                setSessionId(session.id)
                lastSession = session
                reloadStyledLayer()
            }
        }
    }

    @UiThread
    private fun setSessionId(id: String) {
        if (id != lastSession.id) {
            sessionIdSelectorButton.text = id
            lastSession = SessionRecord(id, "", "0")
            storage.save(SESSION_ID, id)
        }
    }

    @UiThread
    private fun setUser(userName: String) {
        userSelectorButton.text = userName
        storage.save(USER_NAME, userName)
    }

    @UiThread
    private fun setStyleSetName(styleSetName: String?) {
        if (styleSetName == null) {
            styleSetNameSelectorButton.text = "Select style set name"
        } else {
            styleSetNameSelectorButton.text = styleSetName
            currentStyleSetName = styleSetName
            reloadStyledLayer()
        }
    }

    @UiThread
    private fun setBranchHead(branchHead: BranchHead?) {
        currentBranchHead = branchHead
        setStyleSetName(getStyleSetNames()?.elementAt(0))
        styleSetNameSelectorButton.isEnabled = branchHead != null
        if (branchHead == null) {
            branchSelectorButton.text = "Select branch"
            styleSetNameSelectorButton.text = "First select a branch"
        } else {
            branchSelectorButton.text = branchHead.branchName
        }
    }

    @UiThread
    private fun getBranchButtonsVisibility(): BranchButtonsVisibility {
        return BranchButtonsVisibility(serviceSelectorButton.isEnabled,
                                        branchSelectorButton.isEnabled,
                                        styleSetNameSelectorButton.isEnabled)
    }

    @UiThread
    private fun setBranchButtonsVisibility(vis: BranchButtonsVisibility) {
        serviceSelectorButton.isEnabled = vis.service
        branchSelectorButton.isEnabled = vis.branch
        styleSetNameSelectorButton.isEnabled = vis.styleSet
    }

    @UiThread
    private fun setService(service: Service) {
        if (currentService != service) {
            serviceSelectorButton.text = service.name
            currentService = service
            branchSelectorButton.isEnabled = true
            setBranchHead(null)
        }
    }

    @UiThread
    private fun setMapMode(mapMode: MapMode) {
        mapview.map.mode = mapMode
        mapModeSelectorButton.text = mapMode.toString()
    }

    @UiThread
    private fun getStyleSetNames(): Array<String>? = currentBranchHead?.styleSetNames

    @UiThread
    private fun inferMapType(styleSetName: String): MapType {
        return if (styleSetName == "skl") MapType.SATELLITE else MapType.NONE
    }

    @UiThread
    private fun reloadStyledLayer() {
        if (branchMode) {
            currentBranchHead?.let{ branchHead -> currentStyleSetName?.let { styleSet ->
                when (tileFormat) {
                    SupportedTileFormat.VEC2 -> reloadVec2StyledLayerWithBranch(styleSet, branchHead.revisionId)
                    SupportedTileFormat.VEC3 -> reloadVec3StyledLayerWithBranch(styleSet, branchHead.revisionId)
                }
                setMapType(inferMapType(styleSet))
            }}
        } else {
            if (lastSession.styleKey != "") {
                when (tileFormat) {
                    SupportedTileFormat.VEC2 -> reloadVec2StyledLayerWithSession(lastSession.styleKey)
                    SupportedTileFormat.VEC3 -> reloadVec3StyledLayerWithSession(lastSession.styleKey)
                }
                setMapType(MapType.NONE)
            }
        }
        if (prodModeEnabled) {
            styledLayer?.activate(false)
            setMapType(MapType.VECTOR_MAP)
        }
        updateStyle()
    }

    @UiThread
    private fun reloadVec2StyledLayerWithSession(styleKey: String) {
        styledLayer?.remove()
        tileUrlProvider = UrlProvider { tileId, _ ->
            "https://${host.config.rendererHost}/tiles"          +
                "?vec_protocol=2"        +
                "&format=protobuf"       +
                "&x=${tileId.x}"         +
                "&y=${tileId.y}"         +
                "&z=${tileId.z}"         +
                "&zmin=${tileId.z}"      +
                "&zmax=${tileId.z}"      +
                "&lang=${locale}"        +
                "&scale=2"               +
                "&mode=basic"            +
                "&maptype=map"           +
                "&style_key=${styleKey}" +
                "&l=map"                 +
                "&layer=map"
        }

        styledLayer = mapview.map.addLayer(
                "DesignMapLayer",
                "application/octet-stream",
                layerOptions,
                tileUrlProvider!!,
                iconUrlProvider,
                modelUrlProvider,
                glyphUrlProvider,
                projection,
                listOf<ZoomRange>())

        styledLayer?.invalidate(version++.toString())
    }

    @UiThread
    private fun reloadVec3StyledLayerWithSession(styleKey: String) {
        styledLayer?.remove()

        tileUrlProvider = UrlProvider { tileId, _ ->
            "https://${host.config.vec3RendererHost}/tiles"     +
                "?vec_protocol=3"   +
                "&format=protobuf"  +
                "&x=${tileId.x}"    +
                "&y=${tileId.y}"    +
                "&z=${tileId.z}"    +
                "&zmin=${tileId.z}" +
                "&zmax=${tileId.z}" +
                "&lang=${locale}"   +
                "&scale=2"          +
                "&source_uri=map"   +
                "&experimental_data_hd=hd_closed_beta"
        }

        vec3StyleUrlProvider = ResourceUrlProvider {
            "https://${host.config.rendererHost}/style"          +
                "?style_key=${styleKey}" +
                "&style_format=2"
        }

        styledLayer = mapview.map.addLayer(
            "DesignMapLayer",
            "application/octet-stream",
            TileFormat.VECTOR3,
            layerOptions,
            tileUrlProvider!!,
            iconUrlProvider,
            modelUrlProvider,
            vec3StyleUrlProvider!!,
            glyphUrlProvider,
            projection,
            listOf<ZoomRange>())

        styledLayer?.invalidate(version++.toString())
    }

    @UiThread
    private fun reloadVec2StyledLayerWithBranch(styleSetName: String, revisionId: Int) {
        styledLayer?.remove()
        tileUrlProvider = UrlProvider { tileId, _ ->
            "https://${host.config.rendererHost}/vmap2/tiles" +
                    "?revision=${revisionId}" +
                    "&styleset=${styleSetName}" +
                    "&x=${tileId.x}" +
                    "&y=${tileId.y}" +
                    "&z=${tileId.z}" +
                    "&zmin=${tileId.z}" +
                    "&zmax=${tileId.z}" +
                    "&lang=${locale}"
        }

        styledLayer = mapview.map.addLayer(
                "DesignMapLayer",
                "application/octet-stream",
                layerOptions,
                tileUrlProvider!!,
                iconUrlProvider,
                modelUrlProvider,
                glyphUrlProvider,
                Projections.getWgs84Mercator(),
                listOf<ZoomRange>())

        styledLayer?.invalidate(version++.toString())
    }

    @UiThread
    private fun reloadVec3StyledLayerWithBranch(styleSetName: String, revisionId: Int) {
        styledLayer?.remove()

        tileUrlProvider = UrlProvider { tileId, _ ->
            "https://${host.config.vec3RendererHost}/tiles" +
                "?x=${tileId.x}" +
                "&y=${tileId.y}" +
                "&z=${tileId.z}" +
                "&zmin=${tileId.z}" +
                "&zmax=${tileId.z}" +
                "&lang=${locale}" +
                "&experimental_data_hd=hd_closed_beta"
        }

        vec3StyleUrlProvider = ResourceUrlProvider {
            "http://${host.config.styleRepoHost}/client_styles" +
                "?revision_id=${revisionId}" +
                "&styleset=${styleSetName}" +
                "&sources=map,terrain"
        }

        styledLayer = mapview.map.addLayer(
            "DesignMapLayer",
            "application/octet-stream",
            TileFormat.VECTOR3,
            layerOptions,
            tileUrlProvider!!,
            iconUrlProvider,
            modelUrlProvider,
            vec3StyleUrlProvider!!,
            glyphUrlProvider,
            Projections.getWgs84Mercator(),
            listOf<ZoomRange>())

        styledLayer?.invalidate(version++.toString())
    }

    private inner class PermanentStorage {
        private val CUSTOMIZATION_SETTINGS_FILE = "customization_settings_file"

        fun save(key: String, value: String) {
            val sharedPref: SharedPreferences = getSharedPreferences(applicationContext)
            val editor: SharedPreferences.Editor = sharedPref.edit()
            editor.putString(key, value)
            editor.apply();
        }

        fun load(key: String): String {
            val sharedPref: SharedPreferences = getSharedPreferences(applicationContext)
            return sharedPref.getString(key, "")!!
        }

        private fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(CUSTOMIZATION_SETTINGS_FILE, Context.MODE_PRIVATE)
        }
    }

    abstract class SelectorDialog : DialogFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val contentView = inflater.inflate(R.layout.dialog_search_layout, null)
            val searchBar: EditText = contentView!!.findViewById(R.id.search_bar)
            val separateLine: View = contentView.findViewById(R.id.dividing_line)
            val contentFrame: ListView = contentView.findViewById(R.id.content_frame)

            fillContentFrame(contentFrame, searchBar, separateLine)

            return contentView
        }

        protected fun <T> dismissIfEmpty(v: ArrayAdapter<T>) {
            if (v.isEmpty)
                dismiss()
        }

        protected abstract fun fillContentFrame(contentFrame: ListView, searchBar: EditText, separateLine: View)
    }

    class SessionIdSelectorDialog : SelectorDialog() {
        protected override fun fillContentFrame(contentFrame: ListView, searchBar: EditText, separateLine: View) {
            val activity = (activity as MapDesignActivity)
            searchBar.visibility = View.GONE
            separateLine.visibility = View.GONE

            val sessions = activity.lastFoundUserSessionIds
            val sessionAdapter = ArrayAdapter<SessionRecord>(activity, R.layout.dialog_item_view, R.id.item_view, sessions)
            dismissIfEmpty(sessionAdapter)
            contentFrame.adapter = sessionAdapter
            contentFrame.setOnItemClickListener { _, _, pos, _ ->
                activity.setSessionId((contentFrame.adapter.getItem(pos) as SessionRecord).id)
                dismiss()
            }
        }
    }

    class UserSelectorDialog : SelectorDialog() {
        protected override fun fillContentFrame(contentFrame: ListView, searchBar: EditText, separateLine: View) {
            val activity = (activity as MapDesignActivity)
            searchBar.visibility = View.GONE
            separateLine.visibility = View.GONE

            val users = activity.lastFoundUsers.toList().sorted()
            val userAdapter = ArrayAdapter<String>(activity, R.layout.dialog_item_view, R.id.item_view, users)
            dismissIfEmpty(userAdapter)
            contentFrame.adapter = userAdapter
            contentFrame.setOnItemClickListener { _, _, pos, _ ->
                activity.setUser((contentFrame.adapter.getItem(pos) as String))
                dismiss()
            }
        }
    }

    class ServiceSelectorDialog : SelectorDialog() {
        protected override fun fillContentFrame(contentFrame: ListView, searchBar: EditText, separateLine: View) {
            val activity = (activity as MapDesignActivity)
            searchBar.visibility = View.GONE
            separateLine.visibility = View.GONE

            val services = activity.services
            val serviceAdapter = ArrayAdapter<Service>(activity, R.layout.dialog_item_view, R.id.item_view, services)
            dismissIfEmpty(serviceAdapter)
            contentFrame.adapter = serviceAdapter
            contentFrame.setOnItemClickListener { _, _, pos, _ ->
                activity.setService((contentFrame.adapter.getItem(pos) as Service))
                dismiss()
            }
        }
    }

    class BranchSelectorDialog : SelectorDialog() {
        protected override fun fillContentFrame(contentFrame: ListView, searchBar: EditText, separateLine: View) {
            val activity = (activity as MapDesignActivity)
            val branches = activity.branches

            val branchHeadsAdapter = ArrayAdapter<BranchHead>(activity, R.layout.dialog_item_view, R.id.item_view, branches)
            dismissIfEmpty(branchHeadsAdapter)
            contentFrame.adapter = branchHeadsAdapter
            contentFrame.setOnItemClickListener { _, _, pos, _ ->
                activity.setBranchHead((contentFrame.adapter.getItem(pos) as BranchHead))
                dismiss()
            }

            searchBar.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    branchHeadsAdapter.filter.filter(s?.toString()) {
                        count -> separateLine.visibility = if (count == 0) View.INVISIBLE else View.VISIBLE
                    }
                }
            })
        }
    }

    class StyleSetNameSelectorDialog : SelectorDialog() {
        protected override fun fillContentFrame(contentFrame: ListView, searchBar: EditText, separateLine: View) {
            val activity = (activity as MapDesignActivity)
            searchBar.visibility = View.GONE
            separateLine.visibility = View.GONE

            val styleSetNames: Array<String> = activity.getStyleSetNames()!!
            val styleSetAdapter = ArrayAdapter<String>(activity, R.layout.dialog_item_view, R.id.item_view, styleSetNames)
            dismissIfEmpty(styleSetAdapter)
            contentFrame.adapter = styleSetAdapter
            contentFrame.setOnItemClickListener { _, _, pos, _ ->
                activity.setStyleSetName((contentFrame.adapter.getItem(pos) as String))
                dismiss()
            }
        }
    }

    class MapModeSelectorDialog : SelectorDialog() {
        protected override fun fillContentFrame(contentFrame: ListView, searchBar: EditText, separateLine: View) {
            val activity = (activity as MapDesignActivity)
            searchBar.visibility = View.GONE
            separateLine.visibility = View.GONE

            val modes = MapMode.values().map { it.toString() }.toTypedArray()
            val modesAdapter = ArrayAdapter<String>(activity, R.layout.dialog_item_view, R.id.item_view, modes)
            dismissIfEmpty(modesAdapter)
            contentFrame.adapter = modesAdapter
            contentFrame.setOnItemClickListener { _, _, pos, _ ->
                activity.setMapMode(MapMode.valueOf(contentFrame.adapter.getItem(pos) as String))
                dismiss()
            }
        }
    }

    class ExtraSettingsDialog : DialogFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val contentView = inflater.inflate(R.layout.map_design_extra_settings, null)
            initFov(contentView)
            initTilt(contentView)
            return contentView
        }

        private fun initFov(contentView: View) {
            val fovSlider = contentView.findViewById<Slider>(R.id.fov_slider)
            val fovView = contentView.findViewById<TextView>(R.id.fov_view)
            fovSlider.value = (activity as MapDesignActivity).mapview.fieldOfViewY.toFloat()
            fovView.text = fovSlider.value.toString()
            fovSlider.addOnChangeListener { _, value, _ ->
                (activity as MapDesignActivity).mapview.fieldOfViewY = value.toDouble()
                fovView.text = value.toString()
            }
        }

        private fun initTilt(contentView: View) {
            val tiltSlider = contentView.findViewById<Slider>(R.id.tilt_slider)
            val tiltView = contentView.findViewById<TextView>(R.id.tilt_view)
            tiltSlider.value = (activity as MapDesignActivity).maxTiltLimit
            tiltView.text = tiltSlider.value.toString()
            tiltSlider.addOnChangeListener { _, value, _ ->
                (activity as MapDesignActivity).updateTiltExperiment(value)
                tiltView.text = value.toString()
            }
        }
    }

    private fun updateStyle() {
        val newStyle = if (useStyleButton.isChecked && !prodModeEnabled) customizationDialog.lastStyle else ""
        styledLayer?.setStyle(0, newStyle)
    }

    private inner class CustomizationDialog {
        var lastStyle = ""

        private val styleHandler = object : MapCustomizationDialog.StyleHandler<StyleTarget> {
            override fun applyStyle(styleTarget: StyleTarget, style: String) {
                lastStyle = style
                this@MapDesignActivity.updateStyle()
            }

            override fun saveStyle(styleTarget: StyleTarget, style: String) {
                applyStyle(styleTarget, style)
            }
        }

        val customizationDialog = MapCustomizationDialog<StyleTarget>(
                this@MapDesignActivity,
                styleHandler,
                mapOf(StyleTarget.BASEMAP to R.menu.basemap_customization_templates))

        fun show() = customizationDialog.show(StyleTarget.BASEMAP, false)
    }

    override fun onObjectTap(event: GeoObjectTapEvent): Boolean {
        event.geoObject.metadataContainer.getItem(GeoObjectSelectionMetadata::class.java)?.let{
            mapview.map.selectGeoObject(it.id, it.layerId)
            return true
        }
        return false
    }

    override fun onMapTap(map: Map, point: Point) {
        map.deselectGeoObject()
    }

    override fun onMapLongTap(map: Map, point: Point) {}

    private fun linkifiedTextView(s: String): TextView {
        val view = TextView(this).apply {
            text = SpannableString(s).also { Linkify.addLinks(it, Linkify.WEB_URLS) }
            movementMethod = LinkMovementMethod.getInstance()
        }
        val paddingInDP: Float = 6.0f
        val paddingInPX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, paddingInDP, resources.displayMetrics).toInt()
        view.setPadding(paddingInPX, paddingInPX, paddingInPX, paddingInPX);
        return view
    }

    @UiThread
    private fun showSevereError(title: String, message: String) {
        if (errorDialogIsShown)
            return
        errorDialogIsShown = true
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(linkifiedTextView(message))
            .setOnDismissListener { errorDialogIsShown = false }
            .create()
            .show()
    }

    @UiThread
    private fun showToast(message: String, time: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(applicationContext, message, time).show()
    }

    private fun <R> withExceptionHandling(func: () -> R): R? {
        return try { func() }
        catch (e: CertificateError) {
            e.message?.let { LOGGER.severe(it) }
            skipNextUpdate = true
            runOnUiThread { showSevereError("Certificate error", e.message ?: e.toString()) }
            null
        }
        catch (e: ConnectionError) {
            e.message?.let { LOGGER.severe(it) }
             runOnUiThread { showToast("Failed to get data from server") }
            null
        }
        catch (e: JSONException) {
            e.message?.let { LOGGER.severe(it) }
            runOnUiThread { showToast("JSON Exception") }
            null
        } catch (e: Exception) {
            LOGGER.severe(e.toString())
            runOnUiThread{ showToast("Unknown error :(", Toast.LENGTH_LONG) }
            null
        }
    }
}
