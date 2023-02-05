ymaps.modules.define(util.testfile(), [
    'Map',
    'control.Button',
    'domEvent.manager',
    'system.browser',
    'expect'
], function (provide, Map, Button, domEventManager, browser) {

    describe('control.Button', function () {
        var map,
            button;

        before(function () {
            map = new Map('map', {
                type: null,
                center: [45, 54],
                zoom: 9
            });
        });

        after(function () {
            map.destroy();
        });

        beforeEach(function () {
            button = new Button('test');
        });

        afterEach(function () {
            button = null;
        });

        it ('Должно корректно отработать множественную вставку-удаление контрола на карту', function () {
            map.controls.add(button);
            map.controls.remove(button);
            map.controls.add(button);
            map.controls.remove(button);
            map.controls.add(button);
            map.controls.remove(button);
        });

        it ('Конструктор должен корректно прокинуть данные в контрол', function () {
            expect(button.data.get('content')).to.be('test');

            button = new Button({
                data: {
                    content: 'test'
                },
                options: {
                    testOption: 'a'
                },
                state: {
                    selected: true
                }
            });

            expect(button.data.get('content')).to.be('test');
            expect(button.options.get('testOption')).to.be('a');
            expect(button.state.get('selected')).to.be(true);
        });

        it('Должен корректно отработать выделение select', function () {
            this.timeout(10000);

            var event;
            button.events.add(['select'], function (e) {
                event = e.get('type');
            });

            button.select();

            expect(button.isSelected()).to.be(true);
            expect(button.state.get('selected')).to.be(true);
            expect(event).to.be('select');
        });

        it('Должен корректно отбработать deselect', function () {
            this.timeout(10000);

            button.select();

            var event;
            button.events.add(['deselect'], function (e) {
                event = e.get('type');
            });

            button.deselect();

            expect(button.isSelected()).to.be(false);
            expect(button.state.get('selected')).to.be(false);
            expect(event).to.be('deselect');
        });

        it('Должен корректно отбработать деактивацию .disable()', function () {
            this.timeout(10000);

            var event;
            button.events.add(['disable'], function (e) {
                event = e.get('type');
            });

            button.disable();

            expect(button.isEnabled()).to.be(false);
            expect(button.state.get('enabled')).to.be(false);
            expect(event).to.be('disable');
        });

        it('Должен корректно отбработать активацию .enable()', function () {
            this.timeout(10000);

            button.disable();

            var event;
            button.events.add(['enable'], function (e) {
                event = e.get('type');
            });

            button.enable();

            expect(button.isEnabled()).to.be(true);
            expect(button.state.get('enabled')).to.be(true);
            expect(event).to.be('enable');
        });

        it('Должен изменить состояние maxWidth при измении значения опции maxWidth', function () {
            map.controls.add(button);

            button.options.set('maxWidth', 999);
            expect(button.state.get('maxWidth')).to.be(999);

            map.controls.remove(button);
        });

        it('Должен обеспечить правильный порядок событий: click - press - select', function (done) {
            this.timeout(10000);

            var events = [];
            button.events.add(['click', 'press', 'select'], function (e) {
                events.push(e.get('type'));
            });

            map.controls.add(button);

            button.getLayout().done(function (buttonLayout) {
                buttonLayout.events.fire('click');
                expect(events[0]).to.be('click');
                expect(events[1]).to.be('press');
                expect(events[2]).to.be('select');

                done();
            });
        });
    });

    provide();
});
