// TODO: статистика
// TODO: верстка

let splitMultiPolygons = (features) => {
    let acc = [];
    for (let i=0; i < features.length; i++) {
        let feature = features[i];
        if (feature.geometry.type == 'MultiPolygon') {
            let result = feature.geometry.coordinates
                .map((polygon) => {
                    return Object.assign({}, feature, {
                        geometry: {
                            type: 'Polygon',
                            coordinates: polygon
                        }
                    });
                });
            acc = acc.concat(result);
        } else {
            acc.push(feature);
        }
    }
    return acc;
};

let getRandomColor = () => {
    let color = [
        Math.floor(Math.random() * 254),
        Math.floor(Math.random() * 254),
        Math.floor(Math.random() * 254)
    ]

    return color
        .map((value) => value.toString(16))
        .map((value) => value.length == 1 ? `0${value}` : value)
        .concat(['ff'])
        .join('');
};

let loadRegionsJson = (regionsData) => {
    let features = {};
    let regions = regionsData.regions;
    let dataset = [];

    for (var i in regions) {
        if (regions.hasOwnProperty(i)) {
            var geometry = window.regionsDecode.geometry(i, regionsData);
            if (regions[i].property) {
                regions[i].property.wikipedia = regions[i].wikipedia;
            }

            const coordinates = geometry.coordinates;

            const geoObject = new ymaps.GeoObject({
                geometry: {
                    type: geometry.type,
                    coordinates: coordinates
                },
                properties: regions[i]
            });

            window.map.geoObjects.add(geoObject);
        }
    }
};

let loadTopoJsonToMap = (tjson) => {
    let geojson = topojson.feature(tjson, tjson.objects[Object.keys(tjson.objects)[0]]);
    loadGeoJsonToMap(geojson);
};

let loadGeoJsonToMap = (geojson) => {
    let om = new ymaps.ObjectManager();
    geojson.features = splitMultiPolygons(geojson.features);
    geojson.features = geojson.features.map((feature, i) => {
        return Object.assign({}, feature, {id: i});
    });
    om.add(geojson);
    window.map.geoObjects.add(om);
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
    ymaps.modules.require(['GeoObjectCollection', 'vow', 'constants.zIndex', 'regions.decode', 'regions.OsmGeoObject', 'Polyline', 'Placemark', 'util.hash.rot13'], (GeoObjectCollection, vow, constantsZIndex, regionsDecode, regionsOsmGeoObject, Polyline, Placemark, utilHashRot13) => {

        window.GeoObjectCollection = GeoObjectCollection;
        window.vow = vow;
        window.constantsZIndex = constantsZIndex;
        window.regionsDecode = regionsDecode;
        window.regionsOsmGeoObject = regionsOsmGeoObject;
        window.Polyline = Polyline;
        window.Placemark = Placemark;
        window.utilHashRot13 = utilHashRot13;

        window.map = new ymaps.Map('map', {
            center: [37.64, 55.76],
            zoom: 5
        });
    
        document
            .querySelector('[name=files]')
            .addEventListener('change', onFileChanged); 
    });

};

// Подсветить зависимые регионы для России (Тюмень + АО, Архангельск + АО)
window.highlightDependentRURegions = (map) => {
    let idTumen = 140291; 
    let idArkhangelsk = 140337;
    let iterator = map.geoObjects.get(0).getIterator();
    let geoObject;
    while ((geoObject = iterator.getNext()) != iterator.STOP_ITERATION) {
        let osmId = geoObject.properties.get('osmId');
        let parents = geoObject.properties.get('parents').map((parent) => parent.id);
        if (osmId == idTumen || parents.indexOf(idTumen) != -1) {
            geoObject.options.set('fillColor', 'ff0000ff');
        }
        if (osmId == idArkhangelsk || parents.indexOf(idArkhangelsk) != -1) {
            geoObject.options.set('fillColor', '00ff00ff');
        }
    }
};

let inRange = (value, target, e) => {
    return value >= (target - e) && value <= (target + e);
};

window.findShards = (map) => {
    let iterator = map.geoObjects.get(0).getIterator();
    let geoObject;
    let max = -Infinity;
    let min = Infinity;
    let e = 0.00002;
    let shards = [];
    while ((geoObject = iterator.getNext()) != iterator.STOP_ITERATION) {
        geoObject.geometry.getCoordinates()
            .forEach((polygon) => {
                let found = false;
                polygon.forEach((coord) => {
                    if (inRange(coord[0], 180, e) || inRange(coord[0], -180, e)) {
                        console.log('Found', shards.length)
                        found = true;
                    }
                });
                if (found) { 
                    shards.push(polygon);
                }
            });
    }
    return shards;
};

window.showShards = (map, shards) => {
    let geoObjects = shards.map((shard, i) => {
        return new ymaps.GeoObject({
            type: 'Feature',
            geometry: {
                type: 'Polygon',
                coordinates: [shard]
            },
            properties: {
                hintContent: `Shard ${i}`
            }
        })
    });

    geoObjects.forEach((geoObject) => {
        geoObject.options.set('fillColor', 'ff0000ff');
        map.geoObjects.add(geoObject);
    });
};

ymaps.ready(() => {
    main();
});
