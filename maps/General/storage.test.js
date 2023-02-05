ymaps.modules.define(util.testfile(), [
    'util.Storage'
], function (provide, Storage) {
    describe('util.storage', function () {
        var storage;
        beforeEach(function () {
            storage = new Storage();
        });

        describe('#get', function () {
            it('Должно вернуться пустое значение', function () {
                expect(storage.get('')).to.not.be.ok();
                expect(storage.get('key')).to.not.be.ok();
            });

            it('Должно вернуться тоже самое значение, если ключ не string', function () {
                var arr = [];
                var obj = {};
                expect(storage.get(arr)).to.eql(arr);
                expect(storage.get(obj)).to.eql(obj);
                expect(storage.get(null)).to.be(null);
            });
        });

        describe('#add', function () {
            beforeEach(function () {
                storage.add('key', 1);
                storage.add('key2', 2);
            });
            it('Должно вернуться сохраненное значение', function () {
                expect(storage.get('key')).to.be(1);
                expect(storage.get('key2')).to.be(2);
            });
        });

        describe('#remove', function () {
            beforeEach(function () {
                storage.add('key', 1);
                storage.add('key2', 2);
                storage.add('key3', 3);
                storage.remove('key');
            });

            it('Должно вернуться пустое значение, если оно было удалено ранее', function () {
                expect(storage.get('key')).to.not.be.ok();
                expect(storage.get('key2')).to.be(2);
            });

            it('Должно вернуться правильное значение при удалении других', function () {
                expect(storage.get('key2')).to.be(2);
            });
        });
    });
    provide();
});
