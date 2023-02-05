ymaps.modules.define(util.testfile(), [
    'Map',
    'behavior.Ruler'
], function (provide, Map, RulerBehavior) {

    describe('behavior.Ruler', function () {

        describe('Установка state', function () {
            var map, ruler;

            beforeEach(function () {
                map = new Map('map', {
                    center: [0, 0],
                    type: "yandex#map",
                    zoom: 3,
                    controls: [],
                    type: null
                });
                ruler = new RulerBehavior();
                ruler.setParent(map.behaviors);
            });

            afterEach(function () {
                map.destroy();
            });

            it('Должен вернуть пустой state', function () {
                expect(ruler.getState()).to.be('');
            });

            it('Должен вернуть пустой state после установки пустого значения', function () {
                ruler.setState();
                expect(ruler.getState()).to.be('');
            });

            it('Должен установить state', function () {

                var state =
                    "-580.07812500,51.80505530~278.43750000,0.00000000~220.78125000," +
                    "-7.52522861~180.00000000,-10.96593526~181.40625000," +
                    "0.00000000~165.93750000,16.71354572";

                ruler.setState(state);

                expect(ruler.geometry.getLength()).to.be(6);
                expect(ruler.getState()).to.be(state);
            });
        });



    });

    provide({});
});
