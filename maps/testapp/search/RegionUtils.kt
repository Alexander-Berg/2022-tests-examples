package com.yandex.maps.testapp.search

import android.widget.ProgressBar
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.offline_cache.Region
import com.yandex.mapkit.offline_cache.RegionListUpdatesListener
import com.yandex.mapkit.offline_cache.RegionListener
import com.yandex.mapkit.offline_cache.RegionState

class RegionUtils {
    companion object {
        fun allRegions(): List<Region> = cacheManager.regions()
        fun downloadedRegions() = allRegions().filter {
            val state = cacheManager.getState(it.id)
            state == RegionState.OUTDATED || state == RegionState.COMPLETED
        }

        private val cacheManager = MapKitFactory.getInstance().offlineCacheManager
    }
}

class RegionDownloadHelper: RegionListener, RegionListUpdatesListener {
    private var progressBars = mapOf<Int, ProgressBar>()
    private var cacheManager = MapKitFactory.getInstance().offlineCacheManager
    private var waitingRegionsToDownload = false

    private val regions : List<Region>
        get() = RegionUtils.allRegions().filter { progressBars.contains(it.id) }
    private fun allRegionsAreReady() = regions.isNotEmpty() &&
        RegionUtils.downloadedRegions()
            .map { it.id }
            .toSet()
            .containsAll(regions.map { it.id })

    var onDownloadStarted = {}
    var onAllRegionsReady = {}

    fun setup(progressBars: Map<Int, ProgressBar>) {
        this.progressBars = progressBars
    }

    fun download() {
        if (waitingRegionsToDownload) { return }
        if (allRegionsAreReady()) {
            onAllRegionsReady()
        } else {
            waitingRegionsToDownload = true
            onDownloadStarted()
            cacheManager.addRegionListener(this)
            cacheManager.addRegionListUpdatesListener(this)
            requestRegionsDownload()
        }
    }

    private fun requestRegionsDownload() {
        if (regions.isEmpty()) { return }
        regions.forEach {
            updateProgress(it.id)
            if (cacheManager.getState(it.id) == RegionState.AVAILABLE) {
                cacheManager.startDownload(it.id)
            }
        }
    }

    override fun onRegionProgress(regionId: Int) = updateProgress(regionId)
    override fun onRegionStateChanged(regionId: Int) {
        if (!waitingRegionsToDownload) { return }
        if (allRegionsAreReady()) {
            cacheManager.removeRegionListUpdatesListener(this)
            cacheManager.removeRegionListener(this)
            onAllRegionsReady()
            waitingRegionsToDownload = false
        }
    }
    override fun onListUpdated() {
        if (!waitingRegionsToDownload) { return }
        requestRegionsDownload()
    }

    private fun updateProgress(regionId: Int) {
        val progress = cacheManager.getProgress(regionId)
        progressBars[regionId]?.progress = (progress * 100).toInt()
    }
}
