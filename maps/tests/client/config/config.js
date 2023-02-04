/**
 * Замоканные данные для тестирования
 */
modules.define('config', [], function (provide) {
    provide({});
});

window.mockConfig = {
    lang: 'ru',
    apiLangParam: 'ru_RU',
    hosts: {
        widget: 'https://api-maps.yandex.ru/services/constructor/',
        mapsapi: 'https://api-maps.yandex.ru/',
        enterprise: 'https://api-maps.yandex.ru/'
    },
    limits: {
        count: {
            // Количественные лимиты
            maps: 500,
            geoObjects: 10000,
            vertexes: 1000,
            updateGeoObjectsPerRequest: 5
        },
        size: {
            // Лимиты на длину записи
            mapName: 256,
            mapDescription: 1024 * 64,
            iconCaption: 256,
            iconContent: 3,
            iconDescription: 1024 * 64
        }
    },
    ymaps: {
        version: '2.1.50',
        params: {
            mode: 'debug'
        }
    },
    user: {
        isUserAuthorized: true
    },
    uatraits: {
        BrowserBase: 'Chromium',
        BrowserBaseVersion: '44.0.2403.97',
        BrowserEngine: 'WebKit',
        BrowserEngineVersion: '537.36',
        BrowserName: 'YandexBrowser',
        BrowserVersion: '15.9.2403.2152',
        OSFamily: 'Windows',
        OSName: 'Windows 10',
        OSVersion: '10.0',
        isBrowser: true,
        isMobile: false,
        x64: true
    },
    export: {
        creator: 'Yandex Map Constructor'
    }
};
