"use strict";

/* global ymaps */
function GeoMotionObject(properties, options) {
  var ymaps = ym;
    /**
     * ����� ���������� geoObject - ��������� ����������� ����������� ����� �� ����� � � ����� �
     *
     * @param {Object}  options
     * @param {Object}  properties
     * @returns {GeoMotionConstructor}
     * @constructor
     */
    function GeoMotionConstructor(properties, options) {
        options = options || {};

        this.waypoints = [];
        // ������� ���������� ������
        this._animate_timeout = 50;

        // ���������� ��������� �������
        this._speedFactor = options.speedFactor || 1;
        this._needAnimationTimeout = options.needAnimationTimeout;

        this._directionsVariants = new DirectionVariants(options.countSides || 4);

        /**
         * ������� ��� �����������.
         *
         * @param {Number} n
         * @constructor
         */
        function DirectionVariants(n) {
            this.n = n;

            this.classes = {
                16: ['e', 'see', 'se', 'sse', 's', 'ssw', 'sw', 'sww', 'w', 'nww', 'nw', 'nnw', 'n', 'nne', 'ne', 'nee'],
                8: ['e', 'se', 's', 'sw', 'w', 'nw', 'n', 'ne'],
                4: ['e', 's', 'w', 'n']
            };
        }

        /**
         * ��������� ���� ��������� ������������ x,y - �����������
         *
         * @param {Number} x
         * @param {Number} y
         *
         * @returns {String}
         */
        DirectionVariants.prototype.getDirection = function (x, y) {
            var n = this.n,
                n2 = this.n >> 1; // half of n

            var number = (Math.round((Math.atan2(y, x) / Math.PI) * n2 + 1 / n) + n2) % n;
            return this.classes[n][number];
        };

        // �������� ����������� ��������
        GeoMotionConstructor.superclass.constructor.call(this, properties, options);
    }

    /**
     * @param {Array} segments - �������� ��������
     */
    function getPointsFromSegments (segments) {
        var points = [];
        var coords;
        var cur;
        var prev;

        if (!segments) {
            throw new Error('segments is undefined');
        }

        /* jshint maxdepth:4 */
        // �������� �������� ��� ���� ���������
        for (var i = 0, l = segments.length; i < l; i++) {
            // ���� ���������� ������ � ����� ��������
            coords = segments[i].getCoordinates();
            // � �������� ������ �� ��� � ������, ����� �������� ������ ������ �����
            for (var j = 0, k = coords.length; j < k; j++) {
                cur = coords[j];
                // ���������� �����
                if (prev &&
                    prev[0].toPrecision(10) === cur[0].toPrecision(10) &&
                    prev[1].toPrecision(10) === cur[1].toPrecision(10)) {
                    continue;
                }

                points.push(cur);
                prev = cur;
            }
        }

        return points;
    }

    ymaps.util.augment(GeoMotionConstructor, ymaps.GeoObject, {
        /**
         * ��������� �� ����������� ����
         * @param {ymaps.Path} path
         * @param {Object} [options]
         * @param {Number} [options.time] - ����� ����������� ��������
         * @param {Number} [options.distance] - ���������
         * @return {Promise}
         */
        moveOnPath: function (path, options) {
            var dfd = this._initDfd();

            this._moveOnPath(path, options).then(function (status) {
                dfd.resolve(status);
            }, function (er) {
                dfd.reject(er);
            });

            return dfd.promise();
        },

        /**
         * ��������� �� ����� � � ����� �
         * @param {Array} points
         * @param {Object} options
         * @param {Number} options.time - ����� ����������� ��������
         * @param {Number} options.distance - ���������
         * @return {Promise}
         */
        moveOnPoint: function (points, options) {
            var dfd = this._initDfd();

            if (!options) {
                return dfd.reject(new Error('options is required'));
            }

            this._moveOnPoint(points, options).then(function (status) {
                dfd.resolve(status);
            }, function (er) {
                dfd.reject(er);
            });

            return dfd.promise();
        },

        /**
         * �������� �� �������� (������ �����)
         * @param {route} paths - ���� �� ����� route.getPaths();
         * @param {Object} [options]
         * @param {Number} [options.time] - ����� ����������� ���� (�������)
         * @param {Number} [options.distance] - ��������� (�����)
         */
        moveOnRoute: function (paths, options) {
            var dfd = this._initDfd();

            this._moveOnRouteStep(paths, 0, ymaps.util.extend({}, options, {
                startAnimationTime: new Date().getTime()
            })).then(function (status) {
                dfd.resolve(status);
            }, function (er) {
                dfd.reject(er);
            });

            return dfd.promise();
        },

        /**
         * �������� �� ������� �����
         * @param {Array} points
         * @param {Object} options
         * @param {Number} options.time - seconds
         * @param {Number} options.distance - meter
         * @returns {*}
         */
        moveOnPoints: function (points, options) {
            var self = this;

            this._initDfd();

            this._moveOnPointStep(points, 0, ymaps.util.extend({}, options, {
                startAnimationTime: new Date().getTime()
            })).then(function (status) {
                self._dfd.resolve(status);
            }, function (er) {
                self._dfd.reject(er);
            });

            return this._dfd.promise();
        },

        /**
         * ������������� �������.
         */
        pause: function () {
            if (!this._dfd || this._isResolved()) {
                return;
            }

            if (this.getState() !== 'moving') {
                return;
            }

            clearTimeout(this._animateTimer);
            // ������ ������� ���������� ����������� - � ������ ������ ������ �� �����
            this.properties.set('state', 'stopped');
        },

        /**
         * ����������� �������.
         */
        resume: function () {
            if (!this._dfd || this._isResolved()) {
                return;
            }

            if (this.getState() === 'stopped') {
                this._runAnimation();
            }
        },

        /**
         * ������������� � ������
         */
        abort: function () {
            if (!this._dfd || this._isResolved()) {
                return;
            }

            this._finished();
            this._dfd.resolve('aborted');
        },

        /**
         * ��������� ��������� �������
         * @returns {String}
         */
        getState: function () {
            return this.properties.get('state');
        },

        _finished: function () {
            clearTimeout(this._animateTimer);
            this.properties.set('state', '');
        },

        /**
         * ��������� ��������. ������ �� ���� ��������� � properties ����������
         *
         * @return {Promise}
         * @private
         */
        _runAnimation: function () {
            var dfd = ymaps.vow.defer();

            // ������ ������� �������
            if (this._animateTimer) {
                clearTimeout(this._animateTimer);
            }

            if (!this._dfdTimer || this._dfdTimer._promise.isResolved()) {
                this._dfdTimer = dfd;
            }

            this.properties.set('state', 'moving');

            this._animateTimer = setInterval(function () {
                var now = new Date().getTime();

                // ���� ����� ������ ��� - ������ ��������
                if (this.waypoints.length === 0) {
                    this._finished();
                    return this._dfdTimer.resolve('completed');
                }

                // ����� ��������� �����
                var nextPoint = this.waypoints.shift();

                if (this._needAnimationTimeout && (now - this._startAnimationTime > this._animationTime)) {
                    nextPoint = this.waypoints.pop() || nextPoint;
                    // ���������� �������
                    this.geometry.setCoordinates(nextPoint.coords);
                    // ������ ������� ���������� ����������� � ���� ��������
                    this.properties.set({direction: nextPoint.direction, deg: nextPoint.deg});

                    this._finished();
                    return this._dfdTimer.resolve('completed');
                }

                // ���������� �������
                this.geometry.setCoordinates(nextPoint.coords);
                // ������ ������� ���������� ����������� � ���� ��������
                this.properties.set({direction: nextPoint.direction, deg: nextPoint.deg});
            }.bind(this), this._animate_timeout);

            return this._dfdTimer.promise();
        },

        _isResolved: function () {
            if (this._dfd) {
                return this._dfd._promise.isResolved();
            }

            return true;
        },

        _initDfd: function () {
            var dfd = ymaps.vow.defer();

            if (!this._dfd || this._isResolved()) {
                this._dfd = dfd;
            }

            return this._dfd;
        },

        _buildRouteAndRunAnimation: function (points, options) {
            var dfd = this._dfd;

            var pathLength = options.distance,
                pathTime = options.time,
                speed = pathLength / pathTime;

            // �� �� ����� ��������� ��������, ���� ������� ��� �� �����
            var map = this.getMap();
            if (!map) {
                return dfd.reject(new Error('The car is not added to the map'));
            }

            var projection = map.options.get('projection');

            var stepSpacing = speed / (1000 / this._animate_timeout) * this._speedFactor;

            // �������� �������
            this.waypoints = this._makeWayPoints(points, stepSpacing, projection);

            this._startAnimationTime = options.startAnimationTime || new Date().getTime();
            this._animationTime = pathTime * 1000;

            return this._runAnimation();
        },

        _moveOnPath: function (path, options) {
            options = ymaps.util.extend({}, {
                distance: path.getLength(),
                time: path.getTime()
            }, options);

            var dfd = this._dfd;

            var segments = path.getSegments();
            if (!segments) {
                return dfd.reject(new Error('No Segments'));
            }

            var points = getPointsFromSegments(segments);

            return this._buildRouteAndRunAnimation(points, options);
        },

        _moveOnPoint: function (points, options) {
            return this._buildRouteAndRunAnimation(points, options);
        },

        /**
         * @param {route} paths
         * @param {Number} index
         * @param {Object} [options]
         * @private
         */
        _moveOnRouteStep: function (paths, index, options) {
            var dfd = ymaps.vow.defer(),
                self = this;

            if (index === paths.getLength()) {
                return dfd.resolve();
            }

            var way = paths.get(index);

            return this._moveOnPath(way, options).then(function () {
                return self._moveOnRouteStep(paths, ++index, options);
            });
        },

        /**
         * @param {Array} points
         * @param {Number} index
         * @param {Object} options
         * @private
         */
        _moveOnPointStep: function (points, index, options) {
            var dfd = ymaps.vow.defer(),
                self = this;

            if (index >= points.length) {
                return dfd.resolve();
            }

            var startPoint = points[index],
                endPoint = points[index + 1];

            return this._moveOnPoint([startPoint, endPoint], options).then(function () {
                return self._moveOnPointStep(points, index += 2, options);
            });
        },

        /**
         * ������������ ���� �������� �� ����, ���, ��� �� �� ��������� ����� ���� �������� ��� ���������
         *
         * @param {Array}   points     - ������ ����� ������������ ���� (���� ������� �� ���������)
         * @param {Number}  stepSpacing  - �������� ����������� ��������� = ���������� / ����� ����
         * @param {Object}  projection
         * @returns {Array}
         */
        _makeWayPoints: function (points, stepSpacing, projection) {
            var coordSystem = projection.getCoordSystem();

            var wayList = [],
            // ���������������
                i, j, l,
                directionsVariants = this._directionsVariants;

            // �������� ����� � �������� ���������
            for (i = 0, l = points.length - 1; l; --l, ++i) {
                var from = points[i],
                    to = points[i + 1],
                    diffX = to[0] - from[0],
                    diffY = to[1] - from[1],
                // ���������� ���������� ����� ��� �������� ��������
                    fromToPixel = projection.toGlobalPixels(from, 10),
                    toToPixel = projection.toGlobalPixels(to, 10),
                    diffXPixel = fromToPixel[0] - toToPixel[0],
                    diffYPixel = fromToPixel[1] - toToPixel[1];

                var direction = directionsVariants.getDirection(diffXPixel, diffYPixel),
                    dist = Math.round(coordSystem.distance(from, to)),
                // ���� �������� �������. ��������� ���������� �������� � �������.
                // ���������� �������� �������� � 90* �.�. ������ � ��� ���������� �� �����
                    deg = Math.round(Math.atan2(diffYPixel, diffXPixel) * 180 / Math.PI) - 90,
                    prop;

                // ������ ������, � �� ������� �������� ���������. ��������� ������� ��������
                for (j = 0; j < dist; j += stepSpacing) {
                    prop = j / dist;
                    wayList.push({
                        coords: [
                            (from[0] + (diffX * prop)).toFixed(6),
                            (from[1] + (diffY * prop)).toFixed(6)
                        ],
                        direction: direction,
                        deg: deg
                    });
                }
            }

            return wayList;
        }
    });

    return new GeoMotionConstructor(properties, options);
}
