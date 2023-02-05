ymaps.modules.define(util.testfile(), [
    'map.action.Sequence',
    'Map',
    'vow'
], function (provide, SequenceAction, Map, vow) {
    describe('map.action.Sequence', function () {
        var map;

        beforeEach(function () {
            map = new Map(util.createMapContainer(), {
                center: [54, 39],
                type: null,
                zoom: 7
            });
        });

        afterEach(function () {
            util.destroyMapAndContainer(map);
        });

        it('should call callback after all ticks are completed', function (done) {
            this.timeout(3000);

            var completedTicks = 0;
            var ticks = [
                {
                    center: [43.130824, 131.933971],
                    duration: 200,
                    zoom: 8,
                    timingFunction: 'ease-out',
                    maxDistance: 1000000,
                    delay: 150,
                    checkZoomRange: true
                },
                {
                    center: [53.203784, 50.193931],
                    duration: 200,
                    zoom: 9,
                    timingFunction: 'ease-in-out',
                    maxDistance: 1000000,
                    delay: 150,
                    checkZoomRange: true
                },
                {
                    center: [-31.366204, 22.540284],
                    duration: 200,
                    zoom: 8,
                    timingFunction: 'ease-in-out',
                    maxDistance: 1000000,
                    delay: 150,
                    checkZoomRange: true
                },
                {
                    center: [40.732537, -74.149647],
                    duration: 200,
                    zoom: 7,
                    timingFunction: 'ease-in',
                    maxDistance: 1000000,
                    delay: 150,
                    checkZoomRange: true
                }
            ];

            ticks.forEach(function (x) {
                x.callback = function() { completedTicks++; };
            });

            var actionCompleted = new vow.Promise(function (resolve) {
                var sequenceAction = new SequenceAction(ticks, { callback: resolve });
                sequenceAction.begin(map.action);
            });

            actionCompleted.then(function () {
                expect(completedTicks).to.be(4);
            }).then(done, done.fail);
        });

        it('should stop previous action when new appears', function () {
            this.timeout(2000);

            var originalCenter = map.getCenter();

            return vow.Promise.resolve()
                .then(function () {
                    return vow.Promise.all([
                        map.panTo([53, 27], { duration: 300,flying: true }),
                        map.setZoom(8, { duration: 300 })
                    ])
                })
                .then(function () {
                    expect(map.getCenter()).to.be.coordinates(originalCenter);
                    expect(map.getZoom()).to.be(8);

                    return vow.Promise.all([
                        map.setZoom(6, { duration: 300 }),
                        map.panTo([54, 39], { duration: 300, flying: true })
                    ]);
                })
                .then(function () {
                    expect(map.getCenter()).to.be.coordinates([54, 39]);
                    expect(map.getZoom()).to.be(8);
                });
        });
    });

    provide({});
});
