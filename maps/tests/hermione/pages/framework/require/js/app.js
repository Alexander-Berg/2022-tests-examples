requirejs.config({
    baseUrl: 'js/lib',
    paths: {
        ymaps: '../../../../../../../init.js?lang=ru_RU',
        jquery: '../../../../../img/2.2.3-jquery.js',
        ready: 'ymaps-ready'
    },
    config: {
        'series-ctl': {
            layers: [
                {
                    "id": "countries",
                    "title": "Страны производства сериалов",
                    "tileUrlTemplate": "countries/%z/%x-%y.png",
                    "notFoundTile": "i/notfound.png",
                    "isTransparent": true,
                    "layerMaxZoom": 5,
                    "layerMinZoom": 3,
                    "legend": '<img src="https://download.yandex.ru/company/figures/2014/series/i/legend-country.png"/>'
                },
                {
                    "id": "men-women",
                    "title": "Мужские и женские сериалы",
                    "tileUrlTemplate": "men-women/%z/%x-%y.png",
                    "notFoundTile": "i/notfound.png",
                    "isTransparent": true,
                    "layerMaxZoom": 5,
                    "layerMinZoom": 3,
                    "legend": '<img src="https://download.yandex.ru/company/figures/2014/series/i/legend-gender.png"/>'
                }
            ]
        },
        'map-view': {
            container: 'YMapsID',
            state: {
                center: [0, 0],
                zoom: 3,
                controls: ['zoomControl']
            },
            options: {
                zoomControlSize: 'small',
                fullscreenControlSize: 'default'
            }
        },
        'legend-map-view': {
            template: '{{ data.content|raw }}',
            position: { bottom: 10, left: 10 }
        }
    },
    map: {
        '*': { jquery: 'jquery-private' },
        'jquery-private': { jquery: 'jquery' }
    },
    shim: {
        ymaps: {
            exports: 'ymaps'
        }
        /*
         jquery: {
         exports: '$'
         }
         */
    },
    waitSeconds: 0
});

require(['series-ctl'], function (SeriesCtl) {
    var series = new SeriesCtl();
});
