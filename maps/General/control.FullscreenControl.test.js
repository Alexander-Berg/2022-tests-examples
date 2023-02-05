ymaps.modules.define(util.testfile(), [
    'control.FullscreenControl',
    'Map',
    'system.browser',
    'expect'
], function (provide, FullscreenControl, Map, browser) {
    describe('control.FullscreenControl', function () {

        var myMap,
            fullscreenControl;

        before(function () {
            myMap = new Map('map', {
                center: [55.777153093859496, 37.639130078124964],
                zoom: 10,
                controls: [],
                type: null
            });
        });

        after(function () {
            myMap.destroy();
        });

        beforeEach(function () {
            fullscreenControl = new FullscreenControl();
            myMap.controls.add(fullscreenControl);
        });

        afterEach(function () {
            myMap.controls.remove(fullscreenControl);
            fullscreenControl = null;
        });

        // Тестирование.
        it('Должен перевести карту в полноэкранный режим', function () {
            fullscreenControl.enterFullscreen();
            expect(myMap.container.isFullscreen()).to.be(true);
        });

        it('Должен вывести карту из полноэкранного режима', function () {
            fullscreenControl.exitFullscreen();
            expect(myMap.container.isFullscreen()).to.be(false);
        });

        it('Должен выбраться (select) при переходе карты в полноэкранный режим', function () {
            myMap.container.enterFullscreen();
            expect(fullscreenControl.isSelected()).to.be(true);
        });

        it('Должен снять выделение (select) после выхода карты из полноэкранного режима', function () {
            myMap.container.exitFullscreen();
            expect(fullscreenControl.isSelected()).not.to.be(true);
        });

        it('Должен перевести карту в полноэкранный режим при select()', function () {
            fullscreenControl.select();
            expect(myMap.container.isFullscreen()).to.be(true);
        });

        it('Должен вывести карту из полноэкранного режима при deselect()', function () {
            myMap.container.enterFullscreen();
            fullscreenControl.deselect();
            expect(myMap.container.isFullscreen()).to.be(false);
        });

        it('Должен пробрасывать события в определенном порядке: click - press - *', function (done) {
            this.timeout(10000);

            var events = [];
            fullscreenControl.events.add(['click', 'press', 'select'], function (e) {
                events.push(e.get('type'));
            });

            fullscreenControl.getLayout().done(function (layout) {
                layout.events.fire('click');

                expect(events[0]).to.be('click');
                expect(events[1]).to.be('press');
                expect(events[2]).to.be('select');

                done();
            });
        });
    });

    provide();
});
