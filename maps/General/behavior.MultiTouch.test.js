ymaps.modules.define(util.testfile(), [
    'behavior.MultiTouch',
    'Map',
    'MapEvent',
    'domEvent.MultiTouch',
    'domEvent.Pointer',
    'domEvent.manager',
    'util.math.areEqual',
    'system.browser',
    'expect'
], function (provide, MultiTouchBehavior, Map, MapEvent, MultiTouchDomEvent, PointerEvent,
    domEventManager, areEqual, browser) {

    describe('behavior.MultiTouch', function () {

        var myMap;
        beforeEach(function () {
            myMap = new Map('map', {
                center: [34, 34],
                zoom: 3,
                controls: [],
                behaviors: ['multiTouch'],
                type: null
            });
        });

        afterEach(function () {
            myMap.destroy();
        });

        if (browser.eventMapper == 'touchMouse') {
            // Тестирование.
            it('Дожен изменить центр карты и зум при пинче', function (done) {
                var oldMapCenter = myMap.getCenter(),
                    oldMapZoom = myMap.getZoom();

                var multitouchEvents = [
                    {
                        type: 'multitouchstart',
                        touches: [[256, 206], [256, 306]],
                        delay: 50
                    },
                    {
                        type: 'multitouchmove',
                        touches: [[384, 156], [384, 356]],
                        delay: 50
                    },
                    {
                        type: 'multitouchend',
                        delay: 50,
                        touches: []
                    }
                ];

                simulateTouchSequence(myMap, multitouchEvents, function () {
                    expect(areEqual(myMap.getCenter(), oldMapCenter, 1e-4)).not.to.be.ok();
                    expect(Math.abs(myMap.getZoom() - oldMapZoom) < 1e-4).not.to.be.ok();
                    done();
                });
            });
        }

        it('При выключении поведения не должен реагировать на события мультитача', function (done) {
            var oldMapCenter = myMap.getCenter(),
                oldMapZoom = myMap.getZoom();

            myMap.behaviors.disable('multiTouch');
            simulateTouchSequence(myMap, [
                {
                    type: 'multitouchstart',
                    touches: [[256, 206], [256, 306]],
                    delay: 50
                },
                {
                    type: 'multitouchmove',
                    touches: [[384, 156], [384, 356]],
                    delay: 50
                },
                {
                    type: 'multitouchend',
                    delay: 50,
                    touches: []
                }
            ], function () {
                expect(areEqual(myMap.getCenter(), oldMapCenter, 1e-6)).to.be.ok();
                expect(Math.abs(myMap.getZoom() - oldMapZoom) < 1e-4).to.be.ok();
                done();
            });
        });

        it('Должен не реагировать на мультитач, который не проходит по опциям multiTouchTremor и multiTouchScaleTremor', function (done) {
            var oldMapCenter = myMap.getCenter(),
                oldMapZoom = myMap.getZoom();

            myMap.options.set({
                multiTouchTremor: 200,
                multiTouchScaleTremor: 10
            });

            simulateTouchSequence(myMap, [
                {
                    type: 'multitouchstart',
                    touches: [[256, 206], [256, 306]],
                    delay: 100
                },
                {
                    type: 'multitouchmove',
                    touches: [[384, 156], [384, 356]],
                    delay: 100
                },
                {
                    type: 'multitouchend',
                    delay: 100,
                    touches: []
                }
            ], function () {
                expect(areEqual(myMap.getCenter(), oldMapCenter, 1e-6)).to.be.ok();
                expect(Math.abs(myMap.getZoom() - oldMapZoom) < 1e-4).to.be.ok();
                done();
            });
        });
    });

    function simulateTouchSequence(map, eventList, callback) {
        var events = eventList.slice();
        next();

        function next() {
            var event = events.shift();
            if (event) {
                simulateTouchEvent(event, map);
                if (event.delay) {
                    window.setTimeout(function () {
                        if (event.asserts) {
                            event.asserts();
                        }
                        next();
                    }, event.delay);
                } else {
                    if (event.asserts) {
                        event.asserts();
                    }
                    next();
                }
            } else {
                if (callback) {
                    callback();
                }
            }
        }

        function simulateTouchEvent(event) {
            var domEvent;
            if (event.type.indexOf('multi') == 0) {
                var touches = [];
                for (var i = 0, l = event.touches.length; i < l; i++) {
                    touches.push({
                        pageX: event.touches[i][0],
                        pageY: event.touches[i][1],
                        clientX: event.touches[i][0],
                        clientY: event.touches[i][1]
                    });
                }
                domEvent = new MultiTouchDomEvent({
                    type: event.type,
                    touches: touches,
                    preventDefault: function () {}
                });
            }
            else if (event.type.indexOf('pointer') == 0) {
                domEvent = new PointerEvent({
                    type: event.type,
                    button: event.button,
                    pointerId: event.pointerId,
                    clientX: event.point[0],
                    clientY: event.point[1],
                    preventDefault: function () {}
                });
            }
            if (event.type == 'multitouchstart') {
                map.events.fire(event.type, new MapEvent({
                    type: event.type,
                    target: map,
                    domEvent: domEvent
                }));
            } else {
                domEventManager.fire(
                    document.documentElement,
                    event.type,
                    domEvent
                );
            }
        }
    }

    provide();
});
