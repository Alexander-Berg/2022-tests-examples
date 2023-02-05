ymaps.modules.define(util.testfile(), [
    "system.browser",
    "domEvent.PointerMapper",
    "util.extend",
    "Event",
    "event.Manager",
    "util.dom.event",

    "domEvent.managerOverrides.pointers",
    "domEvent.multiPointer.override",
    "domEvent.pointer.override"
], function (provide, browser, PointerMapper, extend, Event, EventManager, utilDomEvent) {

    if (browser.name === 'Firefox') {
        provide(true);
        return;
    }

    describe('PointerMapper', function () {

        var element, events, mapper, result,
            timestamp,
            mouseEvents = ['mouseenter', 'mouseleave', 'mousedown',
                           'mouseup', 'mousemove', 'click', 'dblclick', 'contextmenu',
                           'multitouchstart', 'multitouchmove', 'multitouchend'];
        beforeEach(function () {
            timestamp = +(new Date());
            element = new DummyDomElement();
            events = new EventManager({htmlElement: element});
            mapper = new PointerMapper(events);
            mapper.start();
            result = [];
            mapper.events.group()
                .add(mouseEvents, function (e) {
                    result.push(e.get('type'));
                }, this);
        });

        afterEach(function () {
            mapper.stop();
            mapper = null;
            events = null;
            element = null;
        });

        it("Должен обработать наведение пальца", function () {
            element.fire(utilDomEvent.getActualName('pointerover'), {button: -1, pointerId: 1, pointerType: 'pointer'});
            expect(result).to.be.eql([
                'mouseenter'
            ]);
        });

        it("Должен обработать нажатие пальца", function () {
            element.fire(utilDomEvent.getActualName('pointerover'), {button: 0, pointerId: 1, pointerType: 'pointer'});
            element.fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, pointerType: 'pointer'});
            expect(result).to.be.eql([
                'mouseenter',
                'mousemove',
                'mousedown'
            ]);
        });

        it("Должен обработать двойное нажатие", function () {
            element
                .fire(utilDomEvent.getActualName('pointerover'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, timeStamp: timestamp - 5, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, timeStamp: timestamp - 5, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerup'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, timeStamp: timestamp - 5, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, timeStamp: timestamp - 3, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerup'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, timeStamp: timestamp - 3, pointerType: 'pointer'});
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'mouseup', 'click',
                'mousemove', 'mousedown',
                'mouseup', 'click', 'dblclick'
            ]);
        });

        it("Должен обработать два клика", function (done) {
            element
                .fire(utilDomEvent.getActualName('pointerover'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerup'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'});

            setTimeout(function () {
                element
                    .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                    .fire(utilDomEvent.getActualName('pointerup'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'});
                expect(result).to.be.eql([
                    'mouseenter', 'mousemove', 'mousedown',
                    'mouseup', 'click',
                    'mousemove', 'mousedown',
                    'mouseup', 'click'
                ]);
                done();
            }, 1000);
        });

        it("Должен обработать нажатие и перемещение пальца", function () {
            element
                .fire(utilDomEvent.getActualName('pointerover'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointermove'), {button: -1, pointerId: 1, clientX: 4, clientY: 4, detail: 0, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerup'), {button: 0, pointerId: 1, clientX: 4, clientY: 4, pointerType: 'pointer'});
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'mousemove',
                'mouseup'
            ]);
        });

        it("Должен обработать начало множественного прикосновения", function () {
            element
                .fire(utilDomEvent.getActualName('pointerover'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 2, clientX: 1, clientY: 1, pointerType: 'pointer'});
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'multitouchstart'
            ]);
        });

        it("Должен обработать завершение множественного прикосновения", function () {
            element
                .fire(utilDomEvent.getActualName('pointerover'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 2, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerup'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'});
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'multitouchstart',
                'multitouchend'
            ]);
        });

        it("Должен обработать перемещение нескольких прикосновений", function () {
            element
                .fire(utilDomEvent.getActualName('pointerover'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 2, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointermove'), {button: -1, pointerId: 2, clientX: 4, clientY: 4, detail: 0, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointermove'), {button: -1, pointerId: 1, clientX: 5, clientY: 5, detail: 0, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerup'), {button: 0, pointerId: 1, clientX: 4, clientY: 4, pointerType: 'pointer'});
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'multitouchstart',
                'multitouchmove',
                'multitouchmove',
                'multitouchend'
            ]);
        });

        it("Должен обработать ситуацию потери поинтера", function () {
            // Именно в таком порядке события поступают в системе.
            element
                .fire(utilDomEvent.getActualName('pointerover'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerout'), {button: -1, pointerId: 1, pointerType: 'pointer', relatedTarget: null})
                .fire(utilDomEvent.getActualName('pointercancel'), {button: -1, pointerId: 1, pointerType: 'pointer'});
            expect(result).to.be.eql([
                'mouseenter',
                'mousemove', 'mousedown',
                'mouseup', 'mouseleave'
            ]);
        });

        it("Должен обработать ситуацию добавления больше двух пальцев", function () {
            element
                .fire(utilDomEvent.getActualName('pointerover'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 0, pointerId: 1, clientX: 1, clientY: 1, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 1, pointerId: 2, clientX: 2, clientY: 2, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: 2, pointerId: 3, clientX: 3, clientY: 3, pointerType: 'pointer'})
                .fire(utilDomEvent.getActualName('pointerdown'), {button: -1, pointerId: 4, clientX: 4, clientY: 4, pointerType: 'pointer'});
            expect(result).to.be.eql([
                'mouseenter', 'mousemove', 'mousedown',
                'multitouchstart',
                'multitouchend', 'multitouchstart',
                'multitouchend', 'multitouchstart'
            ]);
        });

    });

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
            event.pageX = event.clientX;
            event.pageY = event.clientY;
            this.events.fire(type, event);
            return this;
        };
    }

    provide(true);
});
