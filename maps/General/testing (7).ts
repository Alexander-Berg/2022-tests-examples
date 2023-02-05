import Config from './interface';
import production from './production';
import {YANDEX_BACKOFFICE_PANORAMAS_TESTING} from './urls';

const testing: Config = {
    ...production,
    backendHost: {
        api: 'core-stvbk.testing.maps.yandex.net'
    },
    mapTile: {
        api: 'https://core-stvbk-renderer.testing.maps.yandex.net/session_tiles?%c&'
    },
    panoramaTile: {
        api: 'https://core-stvbk-renderer.testing.maps.yandex.net/tds_tiles?%c&'
    },
    panoramaHotspot: {
        api: 'https://core-stvbk-renderer.testing.maps.yandex.net/tds_hotspots?%c&'
    },
    panoramasHost: {
        api: YANDEX_BACKOFFICE_PANORAMAS_TESTING
    },
    panoramaPreview: {
        api: 'https://core-stvbk-renderer.testing.maps.yandex.net/preview'
    },
    panoramaPhotoHost: {
        api: 'https://core-stvbk-renderer.testing.maps.yandex.net/source'
    }
};

export default testing;
