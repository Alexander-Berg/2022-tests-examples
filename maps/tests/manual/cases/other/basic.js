function init(ym) {

    var map = new ym.Map('map', {
        center: [55.716901946294286, 37.31336807048247],
        zoom: 15,
        behaviors: ['default', 'scrollZoom']
    }, {
        backgroundVisible: false
    });
    var placemark1 = new ymaps.Placemark([55.71574031323344, 37.29753265380539]);
    var placemark2 = new ymaps.Placemark([55.912150224943986, 37.29753265380539]);
    var placemark3 = new ymaps.Placemark([56.16, 37.29753265380539]);
    var placemark4 = new ymaps.Placemark([56.36, 37.29753265380539]);
    var placemark5 = new ymaps.Placemark([55.97, 37.18]);

    var multiRoute = new ymaps.multiRouter.MultiRoute({
        referencePoints: [placemark1.geometry, placemark2.geometry, placemark5.geometry, placemark3.geometry, placemark4.geometry],
        params: {
            viaIndexes: [1, 2],
            reverseGeocoding: true
        }
    }, {
        boundsAutoApply: true
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
        .add(editorButton, {float: 'none', position: {right: 10, bottom: 95}})
        .add(drawOverButton, {float: 'none', position: {left: 5, bottom: 65}})
        .add(clearButton, {float: 'none', position: {right: 10, bottom: 65}});

    var modeButton = new ymaps.control.Button('mode');
    modeButton.events
        .add('select', function () {
            multiRoute.model.setParams({routingMode: 'masstransit'}, true);
        })
        .add('deselect', function () {
            multiRoute.model.setParams({routingMode: 'bicycle'}, true);
        });
    map.controls.add(modeButton, {position: {bottom: 95, left: 5}});
    map.controls.add(new ymaps.control.RouteEditor({options: {}}), {float: 'none', position: {bottom: 125, left: 5}});

    ymaps.geocode('москва').then(function (res) {
            var firstGeoObject = res.geoObjects.get(0),
                coords = firstGeoObject.geometry.getCoordinates(),
                bounds = firstGeoObject.properties.get('boundedBy');
            map.geoObjects.add(res.geoObjects);

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
    map.controls.add(search, {float: 'none', position: {left: 40, top: 45}});
    search.options.set('provider', 'yandex#search');

    var routeButton = new ymaps.control.RouteButton();
    routeButton.routePanel.options.set({
        types: {auto: true, taxi: true, pedestrian: true, masstransit: true}
    });
    map.controls.add(routeButton);

    var reverseButton = new ymaps.control.Button('reverseGeocoding');
    reverseButton.options.set('maxWidth', 99999);
    reverseButton.events
        .add('select', function () {
            multiRoute.model.setParams({reverseGeocoding: true});
        })
        .add('deselect', function () {
            multiRoute.model.setParams({reverseGeocoding: false});
        });

    map.controls.add(reverseButton, {float: 'none', position: {left: 5, bottom: 35}})
    var myPlacemark1 = new ymaps.Placemark([55.80, 37.64], {iconCaption: '<b>fdsafasd</b>fasfasdasd', iconContent: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#sportCircleIcon'}),
        myPlacemark2 = new ymaps.Placemark([55.81, 37.64], {iconCaption: 'Очень длиннный, но невероятно интересный текс.', iconContent: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#sportIcon'}),
        myPlacemark3 = new ymaps.Placemark([55.82, 37.64], {iconCaption: 'Очень длиннный, но невероятно интересный текс.', iconContent: '22'}, {preset: 'islands#icon'}),
        myPlacemark4 = new ymaps.Placemark([55.83, 37.64], {iconCaption: 'Очень длиннный, но невероятно интересный текс.', iconContent: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#blueStretchyIcon'}),
        myPlacemark5 = new ymaps.Placemark([55.84, 37.64], {iconCaption: 'Очень длиннный, но невероятно интересный текс.', iconContent: '22'}, {preset: 'islands#dotIcon'}),
        myPlacemark6 = new ymaps.Placemark([55.85, 37.64], {iconCaption: 'Очень длиннный, но невероятно интересный текс.', iconContent: '22'}, {preset: 'islands#circleIcon'}),
        myPlacemark7 = new ymaps.Placemark([55.80, 37.73], {iconCaption: 'Очень длиннный, но невероятно интересный текс.', iconContent: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#circleDotIcon'}),
        myPlacemark8 = new ymaps.Placemark([55.81, 37.73], {iconCaption: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#circleDotIconWithCaption'}),
        myPlacemark9 = new ymaps.Placemark([55.82, 37.73], {iconCaption: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#dotIconWithCaption'}),

        myPlacemark11 = new ymaps.Placemark([55.86, 37.73],{iconCaption: 'Очень длиннный, но невероятно интересный текс.', iconContent: '22'}),
        myPlacemark10 = new ymaps.Placemark([55.83, 37.73], {iconCaption: 'Очень длиннный, но невероятно интересный текс.', iconContent: '22'}, {
            preset: 'islands#glyphIcon',
            iconGlyph: "tower",
            iconGlyphColor: 'blue',
            iconColor: 'blue'
        }),
        myPlacemark12 = new ymaps.Placemark([55.84, 37.73], {iconCaption: 'Очень длиннный, но невероятно интересный текс.', iconContent: '22'}, {
            preset: 'islands#glyphCircleIcon',
            iconGlyph: "tower",
            iconGlyphColor: 'blue',
            iconColor: 'blue'
        }),
        myPlacemark13 = new ymaps.Placemark([55.85, 37.73], {iconCaption: 'Очень длиннный, но невероятно интересный текс.', iconContent: '22'}, {
            preset: 'islands#blueGlyphIcon',
            iconGlyph: "tower",
            iconGlyphColor: 'blue'
        });

    map.geoObjects
        .add(myPlacemark1)
        .add(myPlacemark2)
        .add(myPlacemark3)
        .add(myPlacemark4)
        .add(myPlacemark5)
        .add(myPlacemark6)
        .add(myPlacemark7)
        .add(myPlacemark8)
        .add(myPlacemark9)
        .add(myPlacemark10)
        .add(myPlacemark11)
        .add(myPlacemark12)
        .add(myPlacemark13);
    var clusterer = new ymaps.Clusterer({
        disableClickZoom: true,
        clusterIconLayout: 'default#pieChart',
        clusterIconPieChartRadius: 25,
        clusterIconPieChartCoreRadius: 10,
        clusterIconPieChartStrokeWidth: 3
    });
    var placemarksNumber = 200;
    var bounds = [[55.11075606205004,42.197696644944706],[56.63160646330035,47.4711341449447]];
    var newPlacemarks = createGeoObjects(placemarksNumber, bounds);
    clusterer.add(newPlacemarks);
    map.geoObjects.add(clusterer);

    function createGeoObjects(number, bounds) {
        var placemarks = [];
        // Создаем нужное количество меток
        for (var i = 0; i < number; i++) {
            // Генерируем координаты метки случайным образом.
            var coordinates = getRandomCoordinates(bounds);
            // Создаем метку со случайными координатами.
            var myPlacemark = new ymaps.Placemark(coordinates, {
                balloonContent: i, hintContent: i, clusterCaption: i}, {iconColor: getRandomColor()});
            placemarks.push(myPlacemark);
        }
        return placemarks;
    }
    function getRandomCoordinates(bounds) {
        var size = [bounds[1][0] - bounds[0][0], bounds[1][1] - bounds[0][1]];
        return [Math.random() * size[0] + bounds[0][0], Math.random() * size[1] + bounds[0][1]];
    }
    function getRandomColor () {
        return [
            '#',
            (55 + Math.round(Math.random() * 200)).toString(16),
            (55 + Math.round(Math.random() * 200)).toString(16),
            (55 + Math.round(Math.random() * 200)).toString(16)
        ].join('')
    }

    var regions = new ymaps.ObjectManager();
    ymaps.borders.load('UA', {
        lang: "uk",
        quality: 1
    }).then(function (res) {
        regions
            .add(res.features.map(function (feature) {
                feature.id = feature.properties.iso3166;
                return feature;
            }));

        map.geoObjects.add(regions);
    })
    ymaps.regions.load('BY', {
        lang: "uk",
        quality: 1
    }).then(function (result) {
        map.geoObjects.add(result.geoObjects)
    })
}