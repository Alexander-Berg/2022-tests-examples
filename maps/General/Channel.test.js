ymaps.modules.define(util.testfile(), [
    'util.imageLoaderComponent.Channel'
], function (provide, Channel) {

    var nullGif = 'data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACwAAAAAAQABAAACAkQBADs=',
        loadTimeout = 500;

    describe('util.imageLoaderComponent.Channel', function () {
        var channel;
        var config = {
            loadLimit: 2,
            maxAtomicLoads: 2
        };

        beforeEach(function () {
            channel = new Channel(config);
        });

        it('Должен поставить картинку в очередь и кинуть запрос на загрузку', function (done) {
            var url = 'someUrl';
            channel.addToQueue({
                url: url,
                priority: 0
            });
            channel.events.once('requestload', function (e) {
                expect(e.get('url')).to.be(url);
                expect(e.get('target')).to.be(channel);
                done();
            });
        });

        it('Не должен просить загрузить картинок больше разрешенного', function () {
            this.timeout(200);
            var url = 'someUrl';
            for (var i = 0; i < 3; i++) {
                channel.addToQueue({
                    url: url + i,
                    priority: 0
                });
            }

            var counter = 0;
            channel.events.add('requestload', function (e) {
                counter++;
                expect(counter).to.be.below(3);
            });
        });

        it('Должен соблюсти порядок загрузки по приоритету', function (done) {
            var url = 'someUrl';
            for (var i = 0; i < 2; i++) {
                channel.addToQueue({
                    url: url + i,
                    priority: i
                });
            }
            channel.events.once('requestload', function (e) {
                expect(e.get('url')).to.be(url + '1');
                done();
            });
        });

        it('Должен отменить загрузку', function () {
            this.timeout(200);
            var url = 'someUrl';
            for (var i = 0; i < 2; i++) {
                channel.addToQueue({
                    url: url + i,
                    priority: i
                });
            }

            channel.cancelLoading(url + '0');
            var counter = 0;
            channel.events.add('requestload', function (e) {
                counter++;
                expect(counter).to.be.below(2);
            });
        });

        it('Должен продолжить загрузку, когда появились свободные слоты', function (done) {
            var url = 'someUrl';
            for (var i = 0; i < 3; i++) {
                channel.addToQueue({
                    url: url + i,
                    priority: 0
                });
            }

            var counter = 0;
            channel.events.add('requestload', function (e) {
                counter++;
                if (counter == 2) {
                    channel.onReady(url + '0');
                }

                if (counter == 3) {
                    expect(e.get('url') == url + '3');
                    done();
                }
            });
        });
    });

    provide();
});
