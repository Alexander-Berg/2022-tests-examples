ymaps.modules.define(util.testfile(), [
    'util.Dragger',
    'DomEvent'
], function (provide, Dragger, DomEvent) {
    describe('util.Dragger', function () {
        it('should correctly map mouse events to start/move/stop', function () {
            var dragger = new Dragger();
            var result = '';

            dragger.events.add(['start', 'move', 'stop'], function (event) {
                var delta = event.get('delta');
                var position = event.get('position');

                result += event.get('type') + ':' +
                    [position[0], position[1], delta[0], delta[1]].join(',') +
                    ' ';
            });

            dragger.start(new DomEvent({
                clientX: 20,
                clientY: 20,
                button: 0
            }, 'mousedown'));

            simulateMouse('mousemove', {clientX: 20, clientY: 20});
            simulateMouse('mousemove', {clientX: 53, clientY: 53});
            simulateMouse('mousemove', {clientX: 99, clientY: 99});
            simulateMouse('mouseup', {clientX: 99, clientY: 99});

            expect(result.trim()).to.be('start:20,20,0,0 move:53,53,33,33 move:99,99,46,46 stop:99,99,0,0');
        });

        it('should fire cancel event', function () {
            var dragger = new Dragger();

            var cancelled;
            dragger.events.add('cancel', function () { cancelled = true; });

            dragger.start(new DomEvent({
                clientX: 30,
                clientY: 50,
                button: 0
            }, 'mousedown'));

            // Смещаем на значение меньше тремора.
            simulateMouse('mousemove', {clientX: 31, clientY: 51});
            simulateMouse('mouseup', {clientX: 31, clientY: 51});

            expect(cancelled).to.be(true);
        });

        it('should fire start if autoStartElement option is set', function () {
            var node = document.createElement('div');
            document.body.appendChild(node);

            var dragger = new Dragger({
                autoStartElement: node
            });

            var started = false;
            dragger.events.add('start', function (event) { started = true; });

            simulateMouse('mousedown', {clientX: 20, clientY: 20}, node);
            simulateMouse('mousemove', {clientX: 20, clientY: 20});
            simulateMouse('mousemove', {clientX: 220, clientY: 220});

            expect(started).to.be(true);
        });

        it('should allow dragging by right mouse button via byRightButton option', function () {
            var node = document.createElement('div');
            document.body.appendChild(node);

            var dragger = new Dragger({
                byRightButton: true,
                autoStartElement: node
            });

            var started = false;
            dragger.events.add('start', function (event) { started = true; });

            simulateMouse('mousedown', {clientX: 20, clientY: 20, button: 2}, node);
            simulateMouse('mousemove', {clientX: 20, clientY: 20, button: 2});
            simulateMouse('mousemove', {clientX: 220, clientY: 220, button: 2});

            expect(started).to.be(true);
        });
    });

    function simulateMouse(type, properties, node) {
        var init = util.extend({
            bubbles: true,
            cancelable: type !== 'mousemove',
            detail: 1
        }, properties, {
            buttons: 1 << properties.button
        });

        var event = document.createEvent("MouseEvents");
        event.initMouseEvent(type,
            init.bubble,
            init.cancelable,
            init.view,
            init.detail,
            init.screenX,
            init.screenY,
            init.clientX,
            init.clientY,
            init.ctrlKey,
            init.altKey,
            init.shiftKey,
            init.metaKey,
            init.button,
            init.relatedTarget);

        (node || document.documentElement).dispatchEvent(event);
    }

    provide({});
});
