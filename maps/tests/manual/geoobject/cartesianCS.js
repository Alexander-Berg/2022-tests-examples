/**
 * User: agoryunova
 * Date: 30.08.13
 * Time: 13:56
 */
/**
 * User: agoryunova
 * Date: 30.07.13
 * Time: 16:21
 */

var cartesianCSSource = function(ymaps, myMap, that) {
    var balloonData = new ymaps.data.Manager({
        balloonContent: 'Content',
        custom: true
    });
    that.geoObjectPlacemark = new ymaps.GeoObject({
        geometry: { type: "Point", coordinates: [30, 0]}, properties: balloonData
    }, {id: 'geoObjectPlacemark', preset: 'twirl#greenIcon'});    that.geoObjectPolyline = new ymaps.GeoObject({
        geometry: { type: "LineString", coordinates: [[30.234375,7.1875],[47.578125,12.890625],[30.3125,17.5]] }, properties: balloonData
    }, { strokeWidth: 3, id: 'geoObjectPolyline', strokeColor: '#00FF00'});
    that.geoObjectCircle = new ymaps.GeoObject({
        geometry: { type: "Circle", coordinates: [40, 30], radius: 10 }, properties: balloonData
    }, {id: 'geoObjectCircle', strokeColor: '#00FF00'});
    that.geoObjectPolygon = new ymaps.GeoObject({
        geometry: { type: "Polygon", coordinates: [[[30, 55], [50, 60], [30, 65]],[[30, 65], [50, 65], [50, 75], [30, 75]]] },
        properties: balloonData}, {id: 'geoObjectPolygon', strokeColor: '#00FF00'});
    that.geoObjectRectangle = new ymaps.GeoObject({
        geometry: { type: "Rectangle", coordinates:[[30.234375,44.0625],[50.234375,49.0625]] }, properties: balloonData
    }, {id: 'geoObjectRectangle', strokeColor: '#00FF00'});
    that.placemark = new ymaps.Placemark([7.578125,0], balloonData, {id: 'placemark'});
    that.polyline = new ymaps.Polyline([[6.171875,6.953125],[24.921875,13.125],[6.249999999999972,17.734375]], balloonData,
        {strokeWidth: 3, id: 'polyline'});
    that.circle = new ymaps.Circle([[15, 30], 10], balloonData, {id: 'circle'});
    that.polygon = new ymaps.Polygon([
        [[5, 55], [25, 60], [5, 65]],
        [[5, 65], [25, 65], [25, 75], [5, 75]]
    ],
        balloonData, {id: 'polygon'});
    that.rectangle = new ymaps.Rectangle([[5,44.0625],[25,49.0625]], balloonData, {id: 'rectangle'});
};