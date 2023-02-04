function main () {
    var PI = Math.PI,
        HALF_PI = 0.5 * PI,
        DOUBLE_PI = 2 * PI;

    function TileLevel (urlTemplate, imageSize, numberOfTiles) {
        this._urlTemplate = urlTemplate;
        this._imageSize = imageSize;
        this._numberOfTiles = numberOfTiles;
    }

    ymaps.util.defineClass(TileLevel, {
        getTileUrl: function (x, y) {
            return this._urlTemplate.replace('%c', this._numberOfTiles[0] * y + x);
        },

        getImageSize: function () {
            return this._imageSize;
        },

        getNumberOfTiles: function () {
            return this._numberOfTiles;
        }
    });

    function Thoroughfare (direction, panorama, connectedPanoramaBaseUrl) {
        this._direction = direction;
        this._panorama = panorama;
        this._connectedPanoramaBaseUrl = connectedPanoramaBaseUrl;
        this.properties = new ymaps.data.Manager();
    }

    ymaps.util.defineClass(Thoroughfare, {
        getDirection: function () {
            return this._direction;
        },

        getPanorama: function () {
            return this._panorama;
        },

        getConnectedPanorama: function () {
            return ymaps.vow.resolve(new Panorama(this._connectedPanoramaBaseUrl));
        }
    });

    var thoroughfares = {
        '1': [
            {
                direction: [4.68, 0],
                connectedPanoramaBaseUrl: '2'
            },
            {
                direction: [6.25, 0],
                connectedPanoramaBaseUrl: '2'
            },
            {
                direction: [1.54, 0],
                connectedPanoramaBaseUrl: '2'
            },
            {
                direction: [3.11, 0],
                connectedPanoramaBaseUrl: '2'
            }
        ],
        '2': [{
            direction: [1.53, 0],
            connectedPanoramaBaseUrl: '1'
        }]
    };

    function renderName (name) {
        var ctx = document.createElement('canvas').getContext('2d');
        ctx.canvas.width = 128;
        ctx.canvas.height = 32;
        ctx.fillStyle = 'white';
        ctx.fillRect(0, 0, 128, 32);
        ctx.fillStyle = 'black';
        ctx.font = '24px Arial';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(name, 64, 16);
        return ctx.canvas;
    }

    function Marker (name, panorama, position) {
        this._position = position;
        this._iconSet = {
            'default': {
                image: renderName(name),
                offset: [64, 16]
            }
        };
        this._panorama = panorama;
        this.properties = new ymaps.data.Manager;
    }

    ymaps.util.defineClass(Marker, {
        getIconSet: function () {
            return ymaps.vow.resolve(this._iconSet);
        },

        getPanorama: function () {
            return this._panorama;
        },

        getPosition: function () {
            return this._position;
        }
    });

    var markers = {
        '1': [{
            name: '@hevil',
            position: [-0.82, -0.57, 0]
        }],
        '2': []
    };

    function Panorama (baseUrl) {
        ymaps.panorama.Base.call(this, { coordSystem: ymaps.coordSystem.cartesian });
        this._tileLevels = [
            new TileLevel(baseUrl + '/lq/%c.jpg', [512, 256], [1, 1]),
            new TileLevel(baseUrl + '/hq/%c.jpg', [7168, 3584], [14, 7])
        ];
        this._thoroughfares = thoroughfares[baseUrl].map(function (thoroughfare) {
            return new Thoroughfare(
                thoroughfare.direction,
                this,
                thoroughfare.connectedPanoramaBaseUrl
            );
        }, this);
        this._markers = markers[baseUrl].map(function (marker) {
            return new Marker(
                marker.name,
                this,
                marker.position
            );
        }, this);
    }

    ymaps.util.defineClass(Panorama, ymaps.panorama.Base, {
        getPosition: function () {
            return [0, 0, 0];
        },

        getAngularBBox: function () {
            return [
                HALF_PI,
                DOUBLE_PI,
                -HALF_PI,
                0
            ];
        },

        getTileSize: function () {
            return [512, 512];
        },

        getTileLevels: function () {
            return this._tileLevels;
        },

        getThoroughfares: function () {
            return this._thoroughfares;
        },

        getMarkers: function () {
            return this._markers;
        }
    });

    window.player = new ymaps.panorama.Player('pano', new Panorama('1'));
}

Api('init', 'coordSystem.cartesian,data.Manager,panorama.Base,panorama.Player,util.defineClass,vow', undefined, undefined, undefined);
    
    function init(ymaps) {
        main();
    }