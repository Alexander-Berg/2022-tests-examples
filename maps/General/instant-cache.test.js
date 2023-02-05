ymaps.modules.define(util.testfile(), [
    'util.instantCache'
], function (provide, instantCache) {
    describe('util.instantCache', function () {
        var cache = instantCache;
        var names = {
            'test': 'value',
            'test2': 'value2'
        };

        beforeEach(function () {
            for (var key in names) {
                cache.add(key, names[key]);
            }
        });

        afterEach(function () {
            for (var key in names) {
                cache.remove(key);
            }
        });

        it('Должен правильно отработать add', function () {
            for (var key in names) {
                expect(cache.get(key)).to.be(names[key]);
            }
        });

        it('Должен правильно отработать remove', function () {
            cache.remove('test2');
            expect(cache.get('test')).to.be(names.test);
            expect(cache.get('test2')).to.not.be.ok();
        });

        it('Должен очиститься кеш', function () {
            setTimeout(function () {
                for (var key in names) {
                    expect(cache.get(key)).to.not.be.ok();
                }
            }, 0);
        });
    });
    provide();
});