ymaps.modules.define('projection.Azimuth', [
    'util.defineClass',
    'util.math.cycleRestrict',
    'coordSystem.Cartesian'
], function (provide, defineClass, cycleRestrict, CoordSystemCartesian) {

    var latLongOrder = false;
    function Azimuth (bounds, cycled, scale) {
        if (true) {
            if (bounds[0][0] == bounds[1][0] || bounds[0][1] == bounds[1][1]) {
                throw new Error("projection.Cartesian: Некорректные значения параметра bounds. Координаты углов не должны совпадать.");
            }
        }

        // в bounds будем хранить прямой порядок координат [x, y]
        if (latLongOrder) {
            bounds = [
                [bounds[0][1], bounds[0][0]],
                [bounds[1][1], bounds[1][0]]
            ];
        }
        this._bounds = bounds;

        this._cycled = cycled ? cycled : [false, false];

        this._coordSystem = new CoordSystemCartesian(scale);
    }

    defineClass(Azimuth, {
        toGlobalPixels: function(point, zoom) {
            var long = point[1],
                lat = point[0],
                centerX = 128 * Math.pow(2,zoom),
                centerY = 128 * Math.pow(2,zoom),
                x,
                y,
                k = 0.71180555555556,
                radius = (90 - lat) * k * Math.pow(2,zoom);
            x = centerX + radius * Math.sin(long*Math.PI/180);
            y = centerY + radius * Math.cos(long*Math.PI/180);
            return [x, y];
        },

        fromGlobalPixels: function(point, zoom) {
            var x = point[0],
                y = point[1],
                centerX = 128 * Math.pow(2,zoom),
                centerY = 128 * Math.pow(2,zoom),
                long,
                lat,
                k = 0.71180555555556,
                protivo = centerX - x,
                prilez = centerY - y;
            long = Math.atan2(-protivo, -prilez)*180/Math.PI;
            lat = 90 - ( Math.sqrt(Math.pow(protivo, 2) + Math.pow(prilez, 2))/(k* Math.pow(2,zoom)));
            return [long, lat];
        },

        isCycled: function () {
            return this._cycled;
        },

        getCoordSystem: function () {
            return this._coordSystem;
        },

        _getFixedPoint: function (geoPoint) {
            var bounds = this._bounds;
            return [
                this._cycled[0] ? cycleRestrict(geoPoint[0], bounds[0][0], bounds[1][0]) : geoPoint[0],
                this._cycled[1] ? cycleRestrict(geoPoint[1], bounds[0][1], bounds[1][1]) : geoPoint[1]
            ];
        }
    });

    provide(Azimuth);
});