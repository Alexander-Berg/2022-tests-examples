ymaps.modules.define(util.testfile(), [
    'objectManager.component.ObjectController',
    'Map',
    'option.Manager'
], function (provide, ObjectController, Map, OptionManager) {

    describe('RemoteObjectManager', function () {
        var map,
            objectController;

        beforeEach(function () {
            map = new Map('map', {
                center: [37.621587, 55.74954],
                zoom: 10,
                controls: [],
                type: null
            });
            objectController = new ObjectController(new OptionManager);
        });

        afterEach(function () {
            map.destroy();
            map = null;
            objectController.destroy();
            objectController = null;
        });

        it('Должен установить и удалить карту', function () {
            objectController.setMap(map);
            objectController.setMap(null);
        });

        it('Должен добавить объекты в контроллер', function () {
            objectController.add([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.74954]
                    },
                    type: 'Feature'
                },
                {
                    id: 2,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.74954]
                    },
                    type: 'Feature'
                }
            ]);
        });

        it('Должен удалить элементы из контроллера', function () {
            objectController.add([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.74954]
                    },
                    type: 'Feature'
                }
            ]);
            objectController.remove([{
                id: 1,
                geometry: {
                    type: 'Point',
                    coordinates: [37.621587, 55.74954]
                },
                type: 'Feature'
            }]);
            objectController.remove([{
                id: 1,
                geometry: {
                    type: 'Point',
                    coordinates: [37.621587, 55.74954]
                },
                type: 'Feature'
            }]);
        });

        it('Должен удалить все элементы из контроллера', function () {
            objectController.add([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.74954]
                    },
                    type: 'Feature'
                }
            ]);
            objectController.removeAll();
        });

        it('Должен добавить объект в контроллер и добавиться на карту', function () {
            objectController.add([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.74954]
                    },
                    type: 'Feature'
                }
            ]);
            objectController.setMap(map);
        });

        it('Должен добавить объект, добавиться на карту, добавить объект', function () {
            objectController.add([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.74954]
                    },
                    type: 'Feature'
                }
            ]);
            objectController.setMap(map);
            objectController.add([
                {
                    id: 2,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.74954]
                    },
                    type: 'Feature'
                }
            ]);
        });

        it('Должен добавить объект, добавиться на карту, удалить объект', function () {
            objectController.add([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.74954]
                    },
                    type: 'Feature'
                }
            ]);
            objectController.setMap(map);
            objectController.remove([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.74954]
                    },
                    type: 'Feature'
                }
            ]);
        });

        it('Должен добавить объект и кинуть событие', function () {
            objectController.add([{
                id: 2,
                geometry: {
                    type: 'Point',
                    coordinates: [37.621587, 55.74954]
                },
                type: 'Feature'
            }]);
            var addedNumber = 0;
            objectController.events.add('statechange', function (e) {
                addedNumber += e.get('added').length;
            });
            objectController.setMap(map);
            expect(addedNumber).to.be(1);
        });

        it('Должен добавить два объекта, удалить один и кинуть соответствующие события', function () {
            objectController.add([{
                id: 1,
                geometry: {
                    type: 'Point',
                    coordinates: [37.621587, 55.74954]
                },
                type: 'Feature'
            }, {
                id: 2,
                geometry: {
                    type: 'Point',
                    coordinates: [37.621587, 55.74954]
                },
                type: 'Feature'
            }]);
            var addedNumber = 0,
                removedNumber = 0;
            objectController.events.add('statechange', function (e) {
                addedNumber += e.get('added').length;
                removedNumber += e.get('removed').length;
            });
            objectController.setMap(map);
            expect(addedNumber).to.be(2);
            objectController.remove([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.74954]
                    },
                    type: 'Feature'
                }
            ]);
            expect(removedNumber).to.be(1);
        });

        it('Должен кинуть корректные события после вызова removeAll', function () {
            var addedNumber = 0,
                removedNumber = 0;
            objectController.events.add('statechange', function (e) {
                addedNumber += e.get('added').length;
                removedNumber += e.get('removed').length;
            });
            objectController.setMap(map);
            objectController.add([{
                id: 1,
                geometry: {
                    type: 'Point',
                    coordinates: [37.621587, 55.74954]
                },
                type: 'Feature'
            }, {
                id: 2,
                geometry: {
                    type: 'Point',
                    coordinates: [37.621587, 55.74954]
                },
                type: 'Feature'
            }]);
            objectController.removeAll();
            expect(addedNumber).to.be(2);
            expect(removedNumber).to.be(2);
        });

        it('Не должен добавить объект вне видимой области карты', function () {
            objectController.add([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [-37.621587, -55.74954]
                    },
                    type: 'Feature'
                }
            ]);
            var addedNumber = 0;
            objectController.events.add('statechange', function (e) {
                addedNumber += e.get('added').length;
            });
            objectController.setMap(map);
            expect(addedNumber).to.be(0);
        });

        it('Не должен кидать событие при удалении объекта вне видимой области карты', function () {
            objectController.add([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [-37.621587, -55.74954]
                    },
                    type: 'Feature'
                }
            ]);
            var addedNumber = 0,
                removedNumber = 0;
            objectController.events.add('statechange', function (e) {
                addedNumber += e.get('added').length;
                removedNumber += e.get('removed').length;
            });
            objectController.setMap(map);
            expect(addedNumber).to.be(0);
            objectController.remove([
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [-37.621587, -55.74954]
                    },
                    type: 'Feature'
                }
            ]);
            expect(removedNumber).to.be(0);
        });

        it('Должен корректно обработать добавление полилинии', function () {
            objectController.add([{
                id: 0,
                geometry: {
                    type: 'LineString',
                    coordinates: [[37.621587, 55.74954], [39.621587, 57.74954]]
                },
                type: 'Feature'
            }]);
            var addedNumber = 0,
                removedNumber = 0;
            objectController.events.add('statechange', function (e) {
                addedNumber += e.get('added').length;
                removedNumber += e.get('removed').length;
            });
            objectController.setMap(map);
            expect(addedNumber).to.be(1);
        });

        it('Должен вернуть null при запросе getBounds, если объекты не добавлены', function () {
            expect(objectController.getBounds()).to.be(null);
        });

        it('Должен корректно вычислить getBounds для точечных объектов', function () {
            objectController.add([{
                id: 2,
                geometry: {
                    type: 'Point',
                    coordinates: [37.621587, 55.74954]
                },
                type: 'Feature'
            }, {
                id: 1,
                geometry: {
                    type: 'Point',
                    coordinates: [37.521587, 55.94954]
                },
                type: 'Feature'
            }]);
            objectController.setMap(map);
            var bounds = objectController.getBounds(),
                expectedBounds = [[37.521587, 55.74954], [37.621587, 55.94954]];
            for (var i = 0; i < 2; i++) {
                for (var j = 0; j < 2; j++) {
                    expect(Math.abs(bounds[i][j] - expectedBounds[i][j])).to.be.lessThan(0.001);
                }
            }
        });

        it('Должен корректно вычислить getBounds для точечных и неточечных объектов', function () {
            objectController.add([{
                id: 2,
                geometry: {
                    type: 'Point',
                    coordinates: [37.621587, 55.74954]
                },
                type: 'Feature'
            }, {
                id: 1,
                geometry: {
                    type: 'Rectangle',
                    coordinates: [[37.521587, 55.84954], [37.571587, 55.94954]]
                },
                type: 'Feature'
            }]);
            objectController.setMap(map);
            var bounds = objectController.getBounds(),
                expectedBounds = [[37.521587, 55.74954], [37.621587, 55.94954]];
            for (var i = 0; i < 2; i++) {
                for (var j = 0; j < 2; j++) {
                    expect(Math.abs(bounds[i][j] - expectedBounds[i][j])).to.be.lessThan(0.001);
                }
            }
        });

        it('Должен правильно сортировать объекты с айдишками разных типов', function () {
            objectController.setMap(map);
            var iter = 0;
            var expected = [1, 2, 'ac', 'l100', 'l101', 'l200'];

            var stateChangeHandler = function(event) {
                if (iter == 0) {
                    expect(event.get('added').length).to.be(6);
                } else {
                    var updated = event.get('update');
                    expect(updated.length).to.be(6);
                    for (var i = 0; i < 6; i++) {
                        expect(updated[i].id).to.be(expected[i]);
                    }
                }
                iter++;
            };

            objectController.events.add('statechange', stateChangeHandler);
            objectController.add([
                {
                    id: 'l101',
                    geometry: {
                        type: 'Point',
                        coordinates: [37.421587, 55.54954]
                    },
                    type: 'Feature'
                },
                {
                    id: 2,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.821587, 55.94954]
                    },
                    type: 'Feature'
                },
                {
                    id: 'l200',
                    geometry: {
                        type: 'Point',
                        coordinates: [37.821587, 55.74954]
                    },
                    type: 'Feature'
                },
                {
                    id: 'ac',
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.94954]
                    },
                    type: 'Feature'
                },
                {
                    id: 1,
                    geometry: {
                        type: 'Point',
                        coordinates: [37.421587, 55.74954]
                    },
                    type: 'Feature'
                },
                {
                    id: 'l100',
                    geometry: {
                        type: 'Point',
                        coordinates: [37.621587, 55.54954]
                    },
                    type: 'Feature'
                },
            ]);
            map.setZoom(11);
            objectController.events.remove('statechange', stateChangeHandler);
        });

        it('Должен хранить один объект, если id в виде числа, но в разных типах', function () {
            //EX: '42' и 42 воспринимаются как одинаковый id
            objectController.setMap(map);
            objectController.add([
                {
                    id: '42',
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 1]
                    },
                    type: 'Feature'
                },
                {
                    id: 42,
                    geometry: {
                        type: 'Point',
                        coordinates: [2, 2]
                    },
                    type: 'Feature'
                }
            ]);
            var allObjects = objectController.getAll();
            expect(allObjects.length).to.be(1);
            expect(typeof allObjects[0].id).to.be('string');
        });
    });

    provide({});
});
