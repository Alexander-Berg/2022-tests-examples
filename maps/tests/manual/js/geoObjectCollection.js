var GeoObjectCollection = function(ns) {
    var collection = new ns.GeoObjectCollection(),
        placemark = new ns.Placemark([35, -37], {
            balloonContent: 'placemark balloon',
            hintContent: 'placemark hint'
        }),
        polygon = new ns.Polygon([
                [[33, -39], [31, -37], [33, -35]],
                [[31, -34], [29, -32], [31, -32]]
            ], {
            balloonContent: 'polygon balloon',
            hintContent: 'polygon hint'
        }),
        rectangle = new ns.Rectangle([[37, -34], [39, -40]], {
            balloonContent: 'rectangle balloon',
            hintContent: 'rectangle hint'
        }),
        circle = new ns.Circle([[35, -29], 300000], {
            balloonContent: 'circle balloon',
            hintContent: 'circle hint'
        }),
        polyline = new ns.Polyline([
                [30, -41], [40, -41]
            ], {
            balloonContent: 'polyline balloon',
            hintContent: 'polyline hint'
        });

    collection.add(placemark).add(polygon).add(rectangle).add(circle).add(polyline);

    collection.getPlacemark = function() {
        return placemark;
    };
    collection.getPolygon = function() {
        return polygon;
    };
    collection.getRectangle = function() {
        return rectangle;
    };
    collection.getCircle = function() {
        return circle;
    };
    collection.getPolyline = function() {
        return polyline;
    };

    return collection;
}