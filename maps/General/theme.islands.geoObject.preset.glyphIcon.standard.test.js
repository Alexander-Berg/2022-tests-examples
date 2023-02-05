ymaps.modules.define(util.testfile(), [
    'Map',
    'Placemark',
    'expect',
    'theme.islands.geoObject.preset.glyphIcon.standard'
], function (provide, Map, Placemark) {
    describe('preset glyphIcon', function () {
        var myMap,
            geoObject;

        before(function () {
            var originalCssLength = document.styleSheets.length;

            var style = document.createElement('link');
            style.setAttribute('rel', 'stylesheet');
            style.setAttribute('type', 'text/css');
            style.setAttribute('href', '/node_modules/bootstrap/dist/css/bootstrap.min.css');
            document.getElementsByTagName('head')[0].appendChild(style);

            return util.poll(function () { return document.styleSheets.length !== originalCssLength; });
        })

        beforeEach(function () {
            myMap = new Map('map', {
                type: null,
                center: [55.755768, 37.617671],
                zoom: 5,
                controls: []
            });
            geoObject = new Placemark(myMap.getCenter(), {}, {
                preset: 'islands#glyphIcon',
                iconGlyph: 'user'
            });
            myMap.geoObjects.add(geoObject);
        });

        afterEach(function () {
            myMap.destroy();
        });

        it('must be dom element with class glyphicon and glyphicon-user', function () {
            return geoObject.getOverlay()
                .then(function() {
                    expect(myMap.container.getElement().getElementsByClassName('glyphicon')).to.have.length(1);
                    expect(myMap.container.getElement().getElementsByClassName('glyphicon-user')).to.have.length(1);
                });
        });

        it('font glyph must be rendered', function () {
            return geoObject.getOverlay()
                .then(util.poll$(function () {
                    var iconElm = myMap.container.getElement().getElementsByClassName('glyphicon')[0];
                    return iconElm.offsetWidth !== 0;
                }, {timeout: 200}))
                .then(function () {
                    var iconElm = myMap.container.getElement().getElementsByClassName('glyphicon')[0];
                    var oldWidth = iconElm.offsetWidth;
                    iconElm.style.fontFamily = 'monospace';

                    expect(iconElm.offsetWidth).to.not.equal(oldWidth);
                });
        });
    });

    provide({});
});
