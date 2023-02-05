ymaps.modules.define(util.testfile(), [
    'objectManager.component.DataStorage',
    'Map',
    'option.Manager'
], function (provide, DataStorage, Map, OptionManager) {
    var map,
        context,
        dataStorage,
        objects = [{
            type: 'Feature',
            geometry: {
                type: 'Point',
                coordinates: [55, 37]
            },
            id: 0
        }, {
            type: 'Feature',
            geometry: {
                type: 'Point',
                coordinates: [55, 37]
            },
            id: 1
        }];

    describe('objectManager.DataStorage', function () {
        beforeEach(function () {
            map = new Map('map', {
                center: [55.755768,37.617671],
                zoom: 15,
                controls: [],
                type: null
            });
            context = {
                getMap: function () {
                    return map;
                },
                options: new OptionManager()
            };
            dataStorage = new DataStorage(context);
        });

        afterEach(function () {
            map.destroy();
            map = null;
            dataStorage = null;
        });

        it('Должен добавить объекты в хранилище', function () {
            dataStorage.add(objects);
            expect(dataStorage.getAll().length).to.be(2);
            expect(dataStorage.getTileData([0, 0], 0).length).to.be(2);
        });

        it('Должен удалить объект из хранилища', function () {
            dataStorage.add(objects);
            dataStorage.remove([objects[1]]);
            expect(dataStorage.getAll().length).to.be(1);
            expect(dataStorage.getTileData([0, 0], 0).length).to.be(1);
        });

        it('Должен удалить все объекты из хранилища', function () {
            dataStorage.add(objects);
            dataStorage.removeAll();
            expect(dataStorage.getAll().length).to.be(0);
            expect(dataStorage.getTileData([0, 0], 0).length).to.be(0);
        });

        it('Должен найти объекты в конкретном тайле', function () {
            var pixelCoordinates = [[120, 120], [300, 300]],
                zoom = 23,
                tileObjects = getObjectsFromPixels(pixelCoordinates, zoom);
            dataStorage.add(tileObjects);
            expect(dataStorage.getTileData([0, 0], zoom).length).to.be(1);
            expect(dataStorage.getTileData([1, 1], zoom).length).to.be(1);
            expect(dataStorage.getTileData([0, 0], 0).length).to.be(2);
            expect(dataStorage.getTileData([0, 0], zoom - 1).length).to.be(2);
            expect(dataStorage.getTileData([3, 3], zoom).length).to.be(0);
        });

        function getObjectsFromPixels (pixelsArray, zoom) {
            var result = [];
            for (var i = 0, l = pixelsArray.length; i < l; i++) {
                result.push({
                    id: i,
                    geometry: {
                        type: 'Point',
                        coordinates: map.options.get('projection').fromGlobalPixels(pixelsArray[i], zoom)
                    }
                });
            }
            return result;
        }
    });
    provide({});
});
