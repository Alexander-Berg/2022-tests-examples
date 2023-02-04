const $ = window.document.querySelector.bind(document);
const encode = encodeURIComponent;

const API_URL = 'https://api-maps.tst.c.maps.yandex.ru/2.1/?lang=ru_RU&apikey=b027f76e-cc66-f012-4f64-696c7961c395';


function getApiURLWithDesignTiles() {
    return API_URL + '&' + [
        `host_config[hosts][mapTiles]=${encode('https://core-renderer-designtesting.maps.n.yandex.ru/tiles?l=map&%c')}`,
        `host_config[hosts][mapjTiles]=${encode('https://core-renderer-designtesting.maps.n.yandex.ru/tiles?l=mapj&%c')}`,
        `host_config[hosts][skljTiles]=${encode('https://core-renderer-designtesting.maps.n.yandex.ru/tiles?l=sklj&%c')}`,
        `host_config[hosts][sklTiles]=${encode('https://core-renderer-designtesting.maps.n.yandex.ru/tiles?l=skl&%c')}`,
        `host_config[hosts][vectorMapTiles]=${encode('https://core-renderer-designtesting.maps.n.yandex.ru/vmap2/')}`,
        `host_config[hosts][vectorTiles]=${encode('https://core-renderer-designtesting.maps.n.yandex.ru/vmap2/tiles?lang={{lang}}&x={{x}}&y={{y}}&z={{z}}&zmin={{zmin}}&zmax={{zmax}}')}`,
        `host_config[hosts][vectorImages]=${encode('https://core-renderer-designtesting.maps.n.yandex.ru/vmap2/icons?id={{id}}&scale={{scale}}')}`,
        `host_config[hosts][vectorMeshes]=${encode('https://core-renderer-designtesting.maps.n.yandex.ru/vmap2/meshes?id={{id}}')}`,
        `host_config[hosts][vectorGlyphs]=${encode('https://core-renderer-designtesting.maps.n.yandex.ru/vmap2/glyphs?lang={{lang}}&font_id={{fontId}}&range={{range}}')}`,
        `host_config[hosts][carparksTileRenderer]=${encode('https://core-carparks-renderer-lots.maps.yandex.net/')}`,
        `host_config[hosts][carparksTiles]=${encode('https://core-carparks-renderer-lots.maps.yandex.net/maps-rdr-carparks/tiles?l=carparks&%c&%l&v=%v')}`,
        `host_config[hosts][traffic]=${encode('https://core-jams-rdr-cache.maps.yandex.net/')}`,
        `host_config[hosts][trafficArchive]=${encode('https://core-jams-rdr-hist.maps.yandex.net/')}`,
        `host_config[inthosts][carparksStamp]=${encode('https://core-carparks-renderer-lots.maps.yandex.net/maps-rdr-carparks/stamp.xml')}`,
        'renderer_experimental_carparks_design=testing',
        'renderer_experimental_designtesting=1',
        'renderer_experimental_trf_design=testing'
    ].join('&');
}

function getApiUrlWithProdTiles() {
    return API_URL + '&' + [
        `host_config[hosts][mapTiles]=${encode('https://core-renderer-tiles.maps.yandex.net/tiles?l=map&%c&%l')}`,
        `host_config[hosts][apiCoverageService]=${encode('https://api-maps.yandex.ru/services/coverage/')}`,
        `host_config[hosts][traffic]=${encode('https://core-jams-rdr-cache.maps.yandex.net/')}`,
        `host_config[hosts][trafficArchive]=${encode('https://core-jams-rdr-hist.maps.yandex.net/')}`,
        `host_config[hosts][stvTiles]=${encode('https://0%d.core-stv-renderer.maps.yandex.net/2.x/tiles?l=stv&%c&v=%v&%l&format=png')}`,
        `host_config[hosts][vectorMapTiles]=${encode('https://core-renderer-tiles.maps.yandex.net/vmap2/')}`,
        `host_config[hosts][vectorTiles]=${encode('https://core-renderer-tiles.maps.yandex.net/vmap2/tiles?lang={{lang}}&x={{x}}&y={{y}}&z={{z}}&zmin={{zmin}}&zmax={{zmax}}&v={{version}}')}`,
        `host_config[hosts][vectorImages]=${encode('https://core-renderer-tiles.maps.yandex.net/vmap2/icons?id={{id}}&scale={{scale}}')}`,
        `host_config[hosts][vectorMeshes]=${encode('https://core-renderer-tiles.maps.yandex.net/vmap2/meshes?id={{id}}')}`,
        `host_config[hosts][vectorGlyphs]=${encode('https://core-renderer-tiles.maps.yandex.net/vmap2/glyphs?lang={{lang}}&font_id={{fontId}}&range={{range}}')}`,
        `host_config[hosts][vectorIndoor]=${encode('https://core-renderer-tiles.maps.yandex.net/')}`
    ].join('&');
}

async function loadApi(apiUrl, namespace) {
    const scriptElement = document.createElement('script');
    scriptElement.src = apiUrl;
    document.head.append(scriptElement);

    await new Promise((resolve) => scriptElement.onload = resolve);

    const [loadEngine, CarParksProvider] = await window.ymaps.modules.require(['vectorEngine.loadEngine', 'carParks.Provider']);

    await loadEngine();

    window[namespace] = window.ymaps;
    window[namespace].carParks = CarParksProvider;
    window.ymaps = undefined;

    return window[namespace];
}

function areCentersEqual(center1, center2) {
    return center1.some((value) => center2.includes(value));
}

function updateCenterAndZoom(center, zoom) {
    if (window.actualMap) {
        window.actualMap.setCenter(center, zoom);
    }
}

function validateCoordinates([lat, long]) {
    return Number.isFinite(lat) &&
        Number.isFinite(long) &&
        Math.abs(lat) <= 90 &&
        Math.abs(long) <= 180;
}

function startLoader() {
    $('.loader').style.display = 'flex';
}

function clearLoader() {
    $('.loader').style.display = 'none';
}

function initDiffContol() {
    const diffSwitcher = $('#diff-checkbox');
    const diffMap = $('#diff-actual');

    let isDiffMode = true;

    diffSwitcher.addEventListener('click', () => {
        isDiffMode = !isDiffMode;

        diffMap.classList.toggle('diff-mode-enabled', isDiffMode);
    });
}

function syncMaps(maps) {
    let mapsReady = 0;

    maps.forEach((map) => {
        map.cursors.push('arrow');

        map.events.add('boundschange', () => {
            maps.forEach((otherMap) => {
                const center = map.getCenter();
                const zoom = map.getZoom();
                const hasChanged = !areCentersEqual(otherMap.getCenter(), center) || otherMap.getZoom() !== zoom;

                if (otherMap !== map && hasChanged) {
                    otherMap.setCenter(center, zoom);

                    $('#zoom-value').textContent = zoom;
                    $('#center-value').textContent = center.map((value) => Number(value).toFixed(4));
                }
            });
        });

        map.layers.events.add('vectorreveal', () => {
            map.layers.get(0).get(0)._vectorLayer._engine.renderLoop.start();
        });

        map.layers.events.add('tileloadchange', (event) => {
            if (event.get('readyTileNumber') === event.get('totalTileNumber')) {
                mapsReady++;

                if (mapsReady === maps.length) {
                    clearLoader();
                }
            }
        });
    });
}

function initMap() {
    startLoader();

    const url = new URL(window.document.location);

    if (!url.searchParams.get('z') || !url.searchParams.get('ll')) {
        console.error('Please specify zoom and center in url like ?z=1&ll=12.345,12.345');
        return;
    }

    const zoom = Number(url.searchParams.get('z'));
    const center = url.searchParams.get('ll').split(',').map((n) => Number(n));
    const mode = url.searchParams.get('mode');
    const mapState = {center, zoom, controls: []};
    const actualMapOptions = {
        vector: true,
        layerVectorMode: mode,
        layerVectorRevealThreshold: 0
    };
    const diffMapOptions = {
        ...actualMapOptions,
        layerVectorDisableBuildingsInteractivity: true,
        layerVectorDisablePoiInteractivity: true
    };

    window.actualMap = new actual.Map('actual', mapState, actualMapOptions);
    window.diffReferenceMap = new diffReference.Map('diff-reference', mapState, diffMapOptions);
    window.diffActualMap = new diffActual.Map('diff-actual', mapState, diffMapOptions);

    const types = (url.searchParams.get('type') || '').split(',').filter(Boolean);

    if (types.includes('carparks')) {
        try {
            const diffReferenceCarParksProvider = new window.diffReference.carParks();
            const diffActualCarParksProvider = new window.diffActual.carParks();
            const actualCarParksProvider = new window.actual.carParks();

            diffReferenceCarParksProvider.setMap(window.diffReferenceMap);
            diffActualCarParksProvider.setMap(window.diffActualMap);
            actualCarParksProvider.setMap(window.actualMap);
        } catch (e) {
            console.error(e);
        }
    }

    if (types.includes('traffic')) {
        try {
            const diffReferenceTrafficProvider = new window.diffReference.traffic.provider.Actual({}, {infoLayerShown: true});
            const diffActualTrafficProvider = new window.diffActual.traffic.provider.Actual({}, {infoLayerShown: true});
            const actualTrafficProvider = new window.actual.traffic.provider.Actual({}, {infoLayerShown: true});

            diffReferenceTrafficProvider.setMap(window.diffReferenceMap);
            diffActualTrafficProvider.setMap(window.diffActualMap);
            actualTrafficProvider.setMap(window.actualMap);
        } catch (e) {
            console.error(e);
        }
    }

    syncMaps([window.actualMap, window.diffReferenceMap, window.diffActualMap]);
}

async function main() {
    await loadApi(getApiUrlWithProdTiles(), 'diffReference');
    await loadApi(getApiURLWithDesignTiles(), 'diffActual');
    await loadApi(getApiURLWithDesignTiles(), 'actual');

    initMap();
    initDiffContol();

    $('#zoom-value').textContent = window.actualMap.getZoom();
    $('#center-value').textContent = window.actualMap.getCenter().map((value) => Number(value).toFixed(4));
};

main();
