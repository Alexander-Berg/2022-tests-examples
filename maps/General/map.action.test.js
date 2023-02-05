ymaps.modules.define(util.testfile(), [
    'Map'
], function (provide, Map) {
    describe('map.action', function () {
        var map;

        function generateBunchOfActions () {
            map.setZoom(4);
            map.setZoom(1);
            map.setZoom(11);
        }

        beforeEach(function () {
            map = new Map(util.createMapContainer(), {
                center: [56, 37],
                zoom: 12,
                type: null
            });
        });

        afterEach(function () {
            util.destroyMapAndContainer(map);
        });

        it('should allow to start new actions in @statechange handler', function () {
            var events = [];

            map.action.events
                // Создаем новые действия до полного завершения предыдущего.
                .once('statechange', generateBunchOfActions)
                .add('begin', function () { events.push('begin'); })
                .add('end', function () { events.push('end'); });

            map.setZoom(9);

            expect(map.getZoom()).to.be(11);
            expect(events.join(' ')).to.be('begin end begin end begin end begin end');
        });

        it('should allow to start new actions in @end handler', function () {
            this.timeout(200);

            var events = [];
            var newEventsCreated = false;

            map.action.events
                .add('begin', function () { events.push('begin'); })
                .add('end', function () {
                    events.push('end');

                    if (!newEventsCreated) {
                        newEventsCreated = true;
                        generateBunchOfActions();
                    }
                });

            map.setZoom(9);

            expect(map.getZoom()).to.be(11);
            expect(events.join(' ')).to.be('begin end begin end begin end begin end');
        });

        it('should allow to start new actions in @tick handler', function () {
            var events = [];

            map.action.events
                // Создаем новые действия до полного заврщения предыдущего.
                .once('tick', generateBunchOfActions)
                .add('begin', function () { events.push('begin'); })
                .add('end', function () { events.push('end'); });

            map.setZoom(9);

            expect(map.getZoom()).to.be(11);
            expect(events.join(' ')).to.be('begin end begin end begin end begin end');
        });

        it('should allow to start new actions in @tickcomplete handler', function () {
            var events = [];

            map.action.events
                // Создаем новые действия до полного заврщения предыдущего.
                .once('tickcomplete', generateBunchOfActions)
                .add('begin', function () { events.push('begin'); })
                .add('end', function () { events.push('end'); });

            map.setZoom(9);

            expect(map.getZoom()).to.be(11);
            expect(events.join(' ')).to.be('begin end begin end begin end begin end');
        });

        it('should allow to start new actions in @begin handler', function () {
            var events = [];
            var newEventsCreated = false;

            map.action.events
                .add('end', function () { events.push('end'); })
                .add('begin', function () {
                    events.push('begin');

                    if (!newEventsCreated) {
                        newEventsCreated = true;
                        generateBunchOfActions();
                    }
                });

            map.setZoom(9);

            expect(map.getZoom()).to.be(11);
            expect(events.join(' ')).to.be('begin end begin end begin end begin end');
        });
    });

    provide({});
});
