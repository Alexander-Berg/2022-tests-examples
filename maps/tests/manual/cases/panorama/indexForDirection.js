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

    function Marker (name, panorama, dir, distance) {
        this._dir = dir;
        this._distance = distance;
        this._iconSet = {
            'default': {
                image: renderName(name),
                offset: [64, 16]
            }
        };
        this._panorama = panorama;
        this.properties = new ymaps.data.Manager();
    }

    ymaps.util.defineClass(Marker, {
        getIconSet: function () {
            return ymaps.vow.resolve(this._iconSet);
        },

        getPanorama: function () {
            return this._panorama;
        },

        getPosition: function () {
            return ymaps.panorama.Base.getMarkerPositionFromDirection(
                this._panorama,
                this._dir,
                this._distance
            );
        }
    });

    var a = 0.5;
    var markers = [[{
            name: '100',
            dir: [4.6,0.01],
            distance: 100
        }],[{
            name: '200',
            dir: [0,0.04],
            distance: 200
        }],[{
            name: '300',
            dir: [0,0.07],
            distance: 300
        }]];

    function Panorama (baseUrl) {
        ymaps.panorama.Base.call(this);
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
        this._markers = markers.map(function (marker) {
            console.log(marker[0])
            return new Marker(
                marker[0].name,
                this,
                marker[0].dir,
                marker[0].distance
            );
        }, this);

        console.log(this._markers);
    }

    ymaps.util.defineClass(Panorama, ymaps.panorama.Base, {
        getPosition: function () {
            return [0, 0, 0];
        },

        getCoordSystem: function () {
            return ymaps.coordSystem.cartesian;
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
    function toFixed2(x) {
        return x.toFixed(2);
    }

    function setState() {
        var dir = player.getDirection().map(toFixed2),
            spn = player.getSpan().map(toFixed2);
        console.log('dir: ' + dir + '; spn: ' + spn)
    }
    player.events
        .add(['directionchange', 'spanchange'], setState);
}

ymaps.ready().done(main);