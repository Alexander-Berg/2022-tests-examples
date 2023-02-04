let loadRegionsJson = (regionsData) => {
    var features = {}, regions = regionsData.regions, dataset = [];
    let time = 0;
    let index = 0;

    for (var i in regions) {
        if (regions.hasOwnProperty(i)) {
            var geometry = window.regionsDecode.geometry(i, regionsData);

            var id;
            if (typeof regions[i].index != 'undefined') {
                id = regions[i].index; 
            } else {
                id = index;
                index += 1;
            }

            if (regions[i].property) {
                regions[i].property.wikipedia = regions[i].wikipedia;
            }

            dataset[id] = new window.regionsOsmGeoObject({
                geometry: geometry,
                properties: regions[i] 
            }, {
                simplificationFixedPoints: geometry.fixedPoints
            });
        }
    }

    var collection = new window.GeoObjectCollection(features, {
        zIndexHover: window.constantsZIndex.overlay
    });

    for (var i = dataset.length - 1; i >= 0; --i) {
        if (dataset[i]) {
            dataset[i].geometry.setFillRule('evenOdd');
            collection.add(dataset[i]);
        } else {
            throw `Leak in indicies i: ${i}`;
        }
    }

    window.map.geoObjects.add(collection);
};

let onFileChanged = (evt) => {
    window.map.geoObjects.removeAll();

    let file = evt.target.files[0];

    let reader = new FileReader();
    reader.onload = (evt) => {
        let str = evt.target.result.replace(/^[^\{]+/, '');
        let json = JSON.parse(str);
        console.log('File loaded', json);
        let ext = file.name.match(/([^\.]+)$/)[1];
        if (ext == 'geojson') {
            loadGeoJsonToMap(json);
        } else if (ext == 'topojson') {
            loadTopoJsonToMap(json);
        } else {
            loadRegionsJson(json);
        }
    };
    console.log('Loading file', file.name);
    reader.readAsText(file);
};

let main = () => {
    ymaps.modules.require(['GeoObjectCollection', 'vow', 'constants.zIndex', 'regions.decode', 'regions.OsmGeoObject', 'Polyline', 'Placemark', 'util.hash.rot13', 'projection.AzimuthalPolarEquidistant'], (GeoObjectCollection, vow, constantsZIndex, regionsDecode, regionsOsmGeoObject, Polyline, Placemark, utilHashRot13, AzimuthalPolarEquidistant) => {

        window.GeoObjectCollection = GeoObjectCollection;
        window.vow = vow;
        window.constantsZIndex = constantsZIndex;
        window.regionsDecode = regionsDecode;
        window.regionsOsmGeoObject = regionsOsmGeoObject;
        window.Polyline = Polyline;
        window.Placemark = Placemark;
        window.utilHashRot13 = utilHashRot13;

        document
            .querySelector('[name=files]')
            .addEventListener('change', onFileChanged); 

        var ARCTIC_LAYER_NAME = 'user#arcticLayer',
            ARCTIC_MAP_TYPE_NAME = 'Арктика',
            ARCTIC_TILES_PATH = 'https://sandbox.api.maps.yandex.net/examples/ru/2.1/azimuthal_map/images/tiles_arctic',
            ARCTIC_PROJECTION = new AzimuthalPolarEquidistant(),
            ANTARCTIC_LAYER_NAME = 'user#antarcticLayer',
            ANTARCTIC_MAP_TYPE_NAME = 'Антарктика',
            ANTARCTIC_TILES_PATH = 'https://sandbox.api.maps.yandex.net/examples/ru/2.1/azimuthal_map/images/tiles_antarctic',
            ANTARCTIC_PROJECTION = new AzimuthalPolarEquidistant(undefined, 4.1583333333333, 0, true),

            /**
             * Конструктор, создающий собственный слой.
             */
            ArcticLayer = function () {
                var layer = new ymaps.Layer(ARCTIC_TILES_PATH + '/%z/tile-%x-%y.jpg', {
                        // Если тайл не загрузился, показываем это изображение.
                        notFoundTile: ARCTIC_TILES_PATH + '/3/tile-0-0.jpg'
                    });
                // Указываем доступный диапазон масштабов для данного слоя.
                layer.getZoomRange = function () {
                    return ymaps.vow.resolve([0, 3]);
                };
                return layer;
            },
            AntarcticLayer = function () {
                var layer = new ymaps.Layer(ANTARCTIC_TILES_PATH + '/%z/tile-%x-%y.jpg', {
                    // Если тайл не загрузился, показываем это изображение.
                    notFoundTile: ANTARCTIC_TILES_PATH + '/3/tile-0-0.jpg'
                });
                // Указываем доступный диапазон масштабов для данного слоя.
                layer.getZoomRange = function () {
                    return ymaps.vow.resolve([0, 4]);
                };
                return layer;
            };

        // Добавляем в хранилище слоев свой конструктор.
        ymaps.layer.storage
            .add(ARCTIC_LAYER_NAME, ArcticLayer)
            .add(ANTARCTIC_LAYER_NAME, AntarcticLayer);

        /**
         * Создадим новый тип карты.
         * MAP_TYPE_NAME - имя нового типа.
         * LAYER_NAME - ключ в хранилище слоев или функция конструктор.
         */
        var mapTypeArctic = new ymaps.MapType(ARCTIC_MAP_TYPE_NAME, [ARCTIC_LAYER_NAME]),
            mapTypeAntarctic = new ymaps.MapType(ANTARCTIC_MAP_TYPE_NAME, [ANTARCTIC_LAYER_NAME]);
        // Сохраняем тип в хранилище типов.
        ymaps.mapType.storage
            .add(ARCTIC_MAP_TYPE_NAME, mapTypeArctic)
            .add(ANTARCTIC_MAP_TYPE_NAME, mapTypeAntarctic);

        /**
         * Создаем карту, указав свой новый тип карты.
         */
        window.map = new ymaps.Map('map', {
                center: [90, 0],
                zoom: 1,
                controls: ["searchControl", "rulerControl"],
                type: ARCTIC_MAP_TYPE_NAME
            }, {
                // Задаем азимутальную проекцию.
                projection: ARCTIC_PROJECTION
            }); 
        var regions;


        var regionsButton = new ymaps.control.Button({
                data: {content: 'Добавить регионы'},
                options: {selectOnClick: true, maxWidth: 150}
            });
        regionsButton.events
            .add('select', function () {
                map.geoObjects.add(regions.geoObjects);
            })
            .add('deselect', function () {
                map.geoObjects.remove(regions.geoObjects);
            });

        var typeButton = new ymaps.control.Button({
                data: {content: 'Антарктика'},
                options: {selectOnClick: true, maxWidth: 150}
            });
        typeButton.events
            .add('select', function () {
                map.setType(ANTARCTIC_MAP_TYPE_NAME);
                map.options.set("projection", ANTARCTIC_PROJECTION);
                typeButton.data.set("content", "Арктика");
            })
            .add('deselect', function () {
                map.setType(ARCTIC_MAP_TYPE_NAME);
                map.options.set("projection", ARCTIC_PROJECTION);
                typeButton.data.set("content", "Антарктика");
            });
        map.controls.add(typeButton);
        ymaps.regions.load('001', {
                lang: 'ru'
            }).then(function (result) {
                regions = result;
                map.controls.add(regionsButton);
            });
        });

};

ymaps.ready(() => {
    main();
});
