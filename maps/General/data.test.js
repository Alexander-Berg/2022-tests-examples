ymaps.modules.define(util.testfile(), [
    'util.data'
], function (provide, data) {
    describe('util.data', function () {
        it('Должен корректно работать с полями объекта', function () {
            var object1 = { id: 1 };
            var object2 = { id: 2 };
            var object3 = object1;
            var count = function (obj) {
                    var l = 0;
                    for (var name in obj) {
                        if (obj.hasOwnProperty(name)) {
                            l++;
                        }
                    }
                    return l;
                };

            expect(data.get(object1, 'test')).not.to.be.ok();

            data.add(object1, 'name', 'value');
            expect(data.get(object1, 'name')).to.be('value');

            data.add(object2, 'name', 'value2');
            expect(data.get(object2, 'name')).to.be('value2');

            data.add(object3, 'name2', 'value2');
            expect(data.get(object1, 'name2')).to.be('value2');
            expect(data.get(object3, 'name')).to.be('value');

            data.remove(object1, 'name2');
            data.remove(object3, 'name');
            data.remove(object2, 'name');

            expect(data.get(object1, 'name')).not.to.be.ok();
            expect(data.get(object1, 'name2')).not.to.be.ok();
            expect(data.get(object2, 'name')).not.to.be.ok();
            expect(count(data.storage)).to.be(0);
        });
    });

    provide({});
});
