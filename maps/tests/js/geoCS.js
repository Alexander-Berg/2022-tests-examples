/**
 * User: agoryunova
 * Date: 30.07.13
 * Time: 16:21
 */

var geoCSSource = function(ymaps, myMap, that) {

    that.geoObjectPlacemark = new ymaps.GeoObject({
        geometry: { type: "Point", coordinates: [55.71574031323344,37.29753265380539] }, properties: {custom: true}
    }, {id: 'geoObjectPlacemark', preset: 'islands#greenIcon'});


    that.geoObjectPolyline = new ymaps.GeoObject({
        geometry: { type: "LineString", coordinates: [
            [55.790716316844474,37.50037536621043],[55.680690559796844,37.442572021483656],[55.788698241203036,37.38720397949171]
        ] }, properties: {custom: false}
    }, { strokeWidth: 3, id: 'geoObjectPolyline', strokeColor: '#00FF00'});

    that.geoObjectCircle = new ymaps.GeoObject({
        geometry: { type: "Circle", coordinates: [55.73211355439117,38.097131347654376], radius: 5000 }, properties: {custom: true}
    }, {id: 'geoObjectCircle', strokeColor: '#00FF00', fillColor: '#00FF00'/*, fillOpacity: 0.5*/});

    that.geoObjectPolygon = new ymaps.GeoObject({
        geometry: { type: "Polygon", coordinates: [[
            [55.787092634473694,37.667663574218096],[55.68797724922255,37.666976928710035],[55.68754896639797,37.57658996581947],[55.78474860376539,37.57684020996037],[55.787092634473694,37.667663574218096]
        ], [
            [55.78709263446105,37.71984863281182],[55.68797724922255,37.666976928710035],[55.68599735621169,37.78258361816322],[55.78709263446105,37.71984863281182]
        ]] }, properties: {custom: false}
    }, {id: 'geoObjectPolygon', strokeColor: '#00FF00', fillColor: '#00FF00'/*, fillOpacity: 0.5*/});

    that.geoObjectRectangle = new ymaps.GeoObject({
        geometry: { type: "Rectangle", coordinates:[
            [55.785448262083506,37.816636657710355],[55.68782209653647,37.945726013178486]
        ] }, properties: {custom: true}
    }, {id: 'geoObjectRectangle', strokeColor: '#00FF00', fillColor: '#00FF00'/*, fillOpacity: 0.5*/});

    that.placemark = new ymaps.Placemark([55.912150224943986,37.29753265380539], {custom: true}, {id: 'placemark'});

    that.polyline = new ymaps.Polyline([
            [55.97596807270264,37.49213562011659],[55.86646804437069,37.43433227538992],[55.97395964586614,37.37896423339792]
        ], {custom: false}, {strokeWidth: 3, id: 'polyline', strokeColor: '#0000FF', fillColor: '#0000FF'});

    that.circle = new ymaps.Circle([[55.9238145091058,38.097131347654376], 5000], {custom: true}, {id: 'circle', strokeColor: '#0000FF', fillColor: '#0000FF'});

    that.polygon = new ymaps.Polygon([[
            [55.97698207150061,37.571533203124304],[55.977780948987515,37.66869354247962],[55.876808118310706,37.66697692871001],[55.87638191482625,37.565603637694494],[55.97698207150061,37.571533203124304]
        ], [
            [55.97544201439153,37.71984863281182],[55.876808118310706,37.66697692871001],[55.8748378377763,37.78258361816322],[55.97544201439153,37.71984863281182]
        ]], {custom: false}, {id: 'polygon', strokeColor: '#0000FF', fillColor: '#0000FF'});

    that.rectangle = new ymaps.Rectangle([
            [55.973805634187,37.81389007567776],[55.87510965298843,37.95396575927215]
        ], {custom: true}, {id: 'rectangle', strokeColor: '#0000FF', fillColor: '#0000FF'});

};