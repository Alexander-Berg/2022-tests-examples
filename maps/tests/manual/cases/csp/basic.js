function init(ym) {

    var map = new ym.Map('map', {
        center: [55.716901946294286, 37.31336807048247],
        zoom: 15,
        behaviors: ['default', 'scrollZoom']
    });
    var placemark1 = new ymaps.Placemark([55.71574031323344, 37.29753265380539]);
    var placemark2 = new ymaps.Placemark([55.912150224943986, 37.29753265380539]);
    var placemark3 = new ymaps.Placemark([56.16, 37.29753265380539]);
    var placemark4 = new ymaps.Placemark([56.36, 37.29753265380539]);
    var placemark5 = new ymaps.Placemark([55.97, 37.18]);

    var multiRoute = new ymaps.multiRouter.MultiRoute({
        referencePoints: [placemark1.geometry, placemark2.geometry, placemark5.geometry, placemark3.geometry, placemark4.geometry],
        params: {
            viaIndexes: [1, 2]
        }
    }, {
        boundsAutoApply: true,
        preset: "islands#multiRouterSmall"
    });

    map.geoObjects.add(multiRoute);

    var clearButton = new ymaps.control.Button('clear');
    var editorButton = new ymaps.control.Button('editor');
    var drawOverButton = new ymaps.control.Button('drawOver');

    editorButton.events
        .add('select', function () {
            multiRoute.editor.start({
                addWayPoints: true,
                dragWayPoints: true,
                dragViaPoints: true,
                removeWayPoints: true,
                removeViaPoints: true
            })
        })
        .add('deselect', function () {
            multiRoute.editor.stop()
        });

    clearButton.events.add('click', function () {
        log.clear();
    });

    drawOverButton.events
        .add('select', function () {
            multiRoute.options.set('editorDrawOver', true)
        })
        .add('select', function () {
            multiRoute.options.set('editorDrawOver', false)
        });

    map.controls
        .add(editorButton)
        .add(drawOverButton)
        .add(clearButton, {float: 'none', position: {right: 10, bottom: 35}});

    var modeButton = new ymaps.control.Button('mode');
    modeButton.events
        .add('select', function () {
            multiRoute.model.setParams({routingMode: 'masstransit'}, true);
        })
        .add('deselect', function () {
            multiRoute.model.setParams({routingMode: 'auto'}, true);
        });
    map.controls.add(modeButton, {position: {bottom: 35, left: 5}});
    map.controls.add(new ymaps.control.RouteEditor({options: {}}), {float: 'none', position: {bottom: 35, left: 10}});

    ymaps.geocode('москва').then(function (res) {
            var firstGeoObject = res.geoObjects.get(0),
                coords = firstGeoObject.geometry.getCoordinates(),
                bounds = firstGeoObject.properties.get('boundedBy');
            map.geoObjects.add(res.geoObjects);
            map.setBounds(bounds, {
                checkZoomRange: true
            });

            /**
             * Все данные в виде javascript-объекта.
             */
            console.log("Все данные геообъекта:");
            console.log('Все данные геообъекта: ');
            console.log(firstGeoObject.properties.getAll());
            /**
             * Метаданные запроса и ответа геокодера.
             * @see https://api.yandex.ru/maps/doc/geocoder/desc/reference/GeocoderResponseMetaData.xml
             */
            console.log('Метаданные ответа геокодера: ');
            console.log(res.metaData);
            /**
             * Метаданные геокодера, возвращаемые для найденного объекта.
             * @see https://api.yandex.ru/maps/doc/geocoder/desc/reference/GeocoderMetaData.xml
             */
            console.log('Метаданные геокодера: ');
            console.log(firstGeoObject.properties.get('metaDataProperty.GeocoderMetaData'));
            /**
             * Точность ответа (precision) возвращается только для домов.
             * @see https://api.yandex.ru/maps/doc/geocoder/desc/reference/precision.xml
             */
            console.log('precision');
            console.log(firstGeoObject.properties.get('metaDataProperty.GeocoderMetaData.precision'));
            /**
             * Тип найденного объекта (kind).
             * @see https://api.yandex.ru/maps/doc/geocoder/desc/reference/kind.xml
             */
            console.log('Тип геообъекта: %s');
            console.log(firstGeoObject.properties.get('metaDataProperty.GeocoderMetaData.kind'));
            console.log('Название объекта: %s');
            console.log(firstGeoObject.properties.get('name'));
            console.log('Описание объекта: %s');
            console.log(firstGeoObject.properties.get('description'));
            console.log('Полное описание объекта: %s');
            console.log(firstGeoObject.properties.get('text'));

        },
        function (err) {
            console.log('Произошла ошибка: ');
            console.log(err.message);
        });


    var search = new ymaps.control.SearchControl();
    map.controls.add(search);
    search.options.set('provider', 'yandex#search');

    var reverseButton = new ymaps.control.Button('reverseGeocoding');
    reverseButton.options.set('maxWidth', 99999);
    reverseButton.events
        .add('select', function () {
            multiRoute.model.setParams({reverseGeocoding: true});
        })
        .add('deselect', function () {
            multiRoute.model.setParams({reverseGeocoding: false});
        });

    map.controls.add(reverseButton)
}