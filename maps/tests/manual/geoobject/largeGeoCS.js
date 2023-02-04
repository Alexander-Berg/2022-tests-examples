/**
 * User: agoryunova
 * Date: 30.07.13
 * Time: 16:21
 */

var largeGeoSource = function(ymaps, myMap, that) {

    that.geoObjectPlacemark = new ymaps.GeoObject({
        geometry: { type: "Point", coordinates: [73.57219711304127,-55.31721343994439] }, properties: {custom: true}
    }, {id: 'geoObjectPlacemark', preset: 'twirl#greenIcon'});


    that.geoObjectPolyline = new ymaps.GeoObject({
        geometry: { type: "LineString", coordinates: [
            [80.3792731826271,-5.236315917969543],[60.8283637757781,-18.626153564454174],[80.4025203117277,-32.546267700196196]
        ] }, properties: {custom: false}
    }, { strokeWidth: 3, id: 'geoObjectPolyline', strokeColor: '#00FF00'});

    that.geoObjectCircle = new ymaps.GeoObject({
        geometry: { type: "Circle", coordinates: [72.75733618873689,149.2921615600492], radius: 300000 }, properties: {custom: true}
    }, {id: 'geoObjectCircle', strokeColor: '#00FF00', fillColor: '#00FF00', fillOpacity: 0.5});

    that.geoObjectPolygon = new ymaps.GeoObject({
        geometry: { type: "Polygon", coordinates: [[
            [80.41623374061939,5.565612792966823],[80.42206763498456,33.862102508542776],[60.25360742162935,35.43984222411875],[59.58276821188885,5.548446655270673],[80.41623374061939,5.565612792966823]
        ], [
            [80.5326124331631,56.8937377929668],[60.25360742162935,35.43984222411875],[60.2884221858358,72.34532165527068],[80.5326124331631,56.8937377929668]
        ]] }, properties: {custom: false}
    }, {id: 'geoObjectPolygon', strokeColor: '#00FF00', fillColor: '#00FF00', fillOpacity: 0.5});

    that.geoObjectRectangle = new ymaps.GeoObject({
        geometry: { type: "Rectangle", coordinates:[
            [79.77912708766131,87.41716156005508],[59.77154007877971,118.35466156005421]
        ] }, properties: {custom: true}
    }, {id: 'geoObjectRectangle', strokeColor: '#00FF00', fillColor: '#00FF00', fillOpacity: 0.5});

    that.placemark = new ymaps.Placemark([-3.3987996221532537,-55.31721343994719], {custom: true}, {id: 'placemark'});

    that.polyline = new ymaps.Polyline([
        [35.153504401957946,-6.642565917969666],[-26.442842374239792,-20.032403564454324],[35.26778093649894,-33.95251770019627]
    ], {custom: false}, {strokeWidth: 3, id: 'polyline'});

    that.circle = new ymaps.Circle([[15.515750894087004,172.4952865600487], 500000], {custom: true}, {id: 'circle'});

    that.polygon = new ymaps.Polygon([[
        [35.335244769265216,11.190612792966778],[35.363956653779724,39.487102508542556],[-27.488341697992304,41.06484222411845],[-28.67336632395747,11.173446655270578],[35.335244769265216,11.190612792966778]
    ], [
        [35.90933730546414,56.8937377929668],[-27.488341697992304,41.06484222411845],[-26.79661208693941,73.0484466552706],[35.90933730546414,56.8937377929668]
    ]], {custom: false}, {id: 'polygon'});

    that.rectangle = new ymaps.Rectangle([
        [35.17965773286704,89.52653656005194],[-27.719764979418862,123.2765365600509]
    ], {custom: true}, {id: 'rectangle'});

};