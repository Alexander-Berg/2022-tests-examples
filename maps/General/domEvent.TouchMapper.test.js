ymaps.modules.define(util.testfile(), [
    "system.browser",
    "domEvent.TouchMapper",
    "util.extend",
    "Event",
    "event.Manager"
], function (provide, browser, TouchMapper, extend, Event, EventManager) {

    if (browser.eventMapper != 'touchMouse') {
        provide(true);
        return;
    }

    describe('TouchMapper', function () {

        var element, events, mapper, result,
            timestamp,
            mouseEvents = ['mouseenter', 'mouseleave', 'mousedown',
                           'mouseup', 'mousemove', 'click', 'dblclick', 'contextmenu',
                           'multitouchstart', 'multitouchmove', 'multitouchend'];
        beforeEach(function () {
            timestamp = +(new Date());
            element = new DummyDomElement();
            events = new EventManager({htmlElement: element});
            mapper = new TouchMapper(events);
            result = [];

            events.add(mouseEvents, function (e) {
                result.push(e.get('type'));
            }, this);

            mapper.start();
        });

        afterEach(function () {
            mapper.stop();
            mapper = null;
            events = null;
            element = null;
        });

        // Проверка преобразований touch-маппера.

        it("Должен обработать движение одного пальца", function () {
            element
                .fire('touchstart', createEvent([
                    [100, 100, 1]
                ], null, timestamp - 6))
                .fire('touchmove', createEvent([
                    [101, 101, 1]
                ], null, timestamp - 5))
                .fire('touchmove', createEvent([
                    [105, 105, 1]
                ], null, timestamp - 3))
                .fire('touchend', createEvent([], [
                    [100, 100, 1]
                ], timestamp));
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'mousemove',
                'mousemove',
                'mouseup', 'mousemove', 'mouseleave']);
        });

        it("Должен обработать единичный таб", function () {
            element
                .fire('touchstart', createEvent([
                    [100, 100, 1]
                ], null, timestamp - 3))
                .fire('touchend', createEvent([], [
                    [100, 100, 1]
                ], timestamp));
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'mouseup', 'click', 'mousemove', 'mouseleave'
            ]);
        });

        it("Должен обработать отмену таба", function () {
            element
                .fire('touchstart', createEvent([
                    [100, 100, 1]
                ], null, timestamp - 3))
                .fire('touchcancel', createEvent([], [
                    [100, 100, 1]
                ], timestamp));
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'mouseup', 'mousemove', 'mouseleave'
            ]);
        });

        it("Должен обработать долгое нажатие", function () {
            element
                .fire('touchstart', createEvent([
                    [100, 100, 1]
                ], null, timestamp - mapper.params.contextMenuTimeout - 1))
                .fire('touchend', createEvent([], [
                    [100, 100, 1]
                ], timestamp));
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'mouseup', 'contextmenu', 'mousemove', 'mouseleave'
            ]);
        });

        it("Должен обработать два быстрых таба", function () {
            element
                .fire('touchstart', createEvent([
                    [100, 100, 1]
                ], null, timestamp - 5))
                .fire('touchend', createEvent([], [
                    [100, 100, 1]
                ], timestamp - 4))
                .fire('touchstart', createEvent([
                    [104, 104, 1]
                ], null, timestamp - 3))
                .fire('touchend', createEvent([], [
                    [104, 104, 1]
                ], timestamp));
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'mouseup', 'click', 'mousemove', 'mouseleave',
                'mouseenter', 'mousemove', 'mousedown',
                'mouseup', 'click', 'dblclick', 'mousemove', 'mouseleave'
            ]);
        });

        it("Должен обработать множественное прикосновение", function () {
            element
                .fire('touchstart', createEvent([
                    [100, 100, 1]
                ], null, timestamp - 5))
                .fire('touchstart', createEvent([
                    [100, 100, 1],
                    [200, 200, 2]
                ], [
                    [200, 200, 2]
                ], timestamp - 5))
                .fire('touchend', createEvent([
                    [200, 200, 2]
                ], [
                    [100, 100, 2]
                ], timestamp - 4))
                .fire('touchend', createEvent([], [
                    [200, 200, 2]
                ], timestamp));
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'multitouchstart', 'multitouchend',
                'mouseup', 'mousemove', 'mouseleave'
            ]);
        });

        it("Должен обработать перемещение нескольких прикосновений", function () {
            element
                .fire('touchstart', createEvent([
                    [100, 100, 1]
                ], null, timestamp - 5))
                .fire('touchstart', createEvent([
                    [100, 100, 1],
                    [200, 200, 2]
                ], [
                    [200, 200, 2]
                ], timestamp - 5))
                .fire('touchmove', createEvent([
                    [100, 100, 1],
                    [200, 200, 2]
                ], [
                    [200, 200, 2]
                ], timestamp - 4))
                .fire('touchmove', createEvent([
                    [100, 100, 1],
                    [200, 200, 2]
                ], [
                    [200, 200, 2]
                ], timestamp - 3))
                .fire('touchmove', createEvent([
                    [100, 100, 1],
                    [200, 200, 2]
                ], [
                    [200, 200, 2]
                ], timestamp - 2))
                .fire('touchend', createEvent([
                    [200, 200, 2]
                ], [
                    [100, 100, 2]
                ], timestamp - 2))
                .fire('touchend', createEvent([], [
                    [200, 200, 2]
                ], timestamp - 1));
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'multitouchstart',
                'multitouchmove',
                'multitouchmove',
                'multitouchmove',
                'multitouchend',
                'mouseup', 'mousemove', 'mouseleave'
            ]);
        });

        it("Должен обработать ситуацию более двух прикосновений", function () {
            element
                .fire('touchstart', createEvent([
                    [100, 100, 1]
                ], null, timestamp - 5))
                .fire('touchstart', createEvent([
                    [100, 100, 1],
                    [200, 200, 2]
                ], [
                    [200, 200, 2]
                ], timestamp - 5))
                .fire('touchstart', createEvent([
                    [100, 100, 1],
                    [200, 200, 2],
                    [150, 150, 3]
                ], [
                    [150, 150, 3]
                ], timestamp - 5))
                .fire('touchend', createEvent([
                    [100, 100, 1],
                    [150, 150, 3]
                ], [
                    [200, 200, 2]
                ], timestamp - 3))
                .fire('touchend', createEvent([
                    [100, 100, 1]
                ], [
                    [150, 150, 3]
                ], timestamp - 4))
                .fire('touchend', createEvent([], [
                    [100, 100, 1]
                ], timestamp));
            expect(result).to.be.eql([
                // Поставили первый палец
                'mouseenter', 'mousemove', 'mousedown',
                // Второй
                'multitouchstart',
                // Добавили третий
                'multitouchend',
                'multitouchstart',
                // Убрали третий
                'multitouchend',
                'multitouchstart',
                // Убрали второй
                'multitouchend',
                // Убрали последний
                'mouseup', 'mousemove', 'mouseleave'
            ]);
        });

        // TODO + тесты на события в документе
        
        function DummyDomElement () {
            this.events = new EventManager();

            this.addEventListener = function (type, callback) {
                this.events.add(type, callback);
            };

            this.removeEventListener = function (type, callback) {
                this.events.remove(type, callback);
            };

            this.fire = function (type, originalEvent) {
                var event = new Event();
                event.type = type;
                event = extend(event, originalEvent);
                this.events.fire(type, event);
                return this;
            };
        }

        function createEvent (touches, changedTouches, timestamp) {
            var result = {
                timeStamp: timestamp,
                touches: [],
                changedTouches: []
            };

            if (!changedTouches) {
                changedTouches = touches;
            }

            for (var i = 0, l = touches.length; i < l; i++) {
                result.touches.push({
                    identifier: touches[i][2],
                    pageX: touches[i][0],
                    pageY: touches[i][1],
                    clientX: touches[i][0],
                    clientY: touches[i][1]
                });
            }

            for (var i = 0, l = changedTouches.length; i < l; i++) {
                result.changedTouches.push({
                    identifier: changedTouches[i][2],
                    pageX: changedTouches[i][0],
                    pageY: changedTouches[i][1],
                    clientX: changedTouches[i][0],
                    clientY: changedTouches[i][1]
                });
            }

            return result;
        }
    });

    provide(true);
});
