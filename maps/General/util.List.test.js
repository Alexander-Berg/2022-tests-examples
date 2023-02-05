ymaps.modules.define(util.testfile(), [
    'util.List',
    'expect'
], function (provide, List, expect) {
    describe('util.List', function () {

        it('Должен корректно выполнять insert', function () {
            var hList = new List(),
                obj1 = { name: 'a' },
                obj2 = { name: 'b' },
                obj3 = { name: 'c' },
                obj4 = { name: 'd' },
                test = function () {
                    var res = '',
                        elem,
                        obj;
                    for (elem = hList.first; elem; elem = elem.next) {
                        res += elem.obj.name;
                    }
                    return res;
                };

            hList.insert(obj2, null);
            hList.insert(obj4);
            expect(test()).to.be('bd');

            hList.insert(obj1, obj2);
            hList.insert(obj3, obj4);
            expect(test()).to.be('abcd');

            hList.insert(obj3, obj1);
            expect(test()).to.be('cabd');
        });

        it('Должен отработать remove', function () {
            var hList = new List(),
                obj1 = { name: 'a' },
                obj2 = { name: 'b' },
                obj3 = { name: 'c' },
                obj4 = { name: 'd' },
                test = function () {
                    var res = '',
                        elem,
                        obj;
                    for (elem = hList.first; elem; elem = elem.next) {
                        res += elem.obj.name;
                    }
                    return res;
                };
            hList.insert(obj1);
            hList.insert(obj2);
            hList.insert(obj3);
            hList.insert(obj4);
            hList.remove({ name: 'e'});
            expect(test()).to.be('abcd');

            hList.removeAll();
            expect(test()).to.be('');
        });

        it('Должен проверить итератор', function () {
            var hList = new List(),
                obj1 = { name: 'a' },
                obj2 = { name: 'b' },
                obj3 = { name: 'c' },
                obj4 = { name: 'd' },
                test = function () {
                    var res = '',
                        elem,
                        obj;
                    for (elem = hList.first; elem; elem = elem.next) {
                        res += elem.obj.name;
                    }
                    return res;
                };
            hList.insert(obj1);
            hList.insert(obj2);
            hList.insert(obj3);
            hList.insert(obj4);
            var res = '',
                it = hList.getIterator(),
                obj;
            while ((obj = it.getNext()) != it.STOP_ITERATION) {
                if (obj == obj2) {
                    hList.remove(obj1);
                    hList.remove(obj2);
                }
                if (obj == obj3) {
                    res = 'c';
                }
            }
            expect(res).to.be('c');
            expect(test()).to.be('cd');

            hList.insert(obj2, obj3);
            hList.insert(obj1, obj2);

            it = hList.getIterator();
            while ((obj = it.getNext()) != it.STOP_ITERATION) {
                if (obj == obj2) {
                    hList.removeAll();
                }
            }
            expect(test()).to.be('');

            obj = it.getNext();
            expect(obj).to.be(it.STOP_ITERATION);
        });


        it('Должен проверить итератор', function () {
            var list = new List(),
                obj1 = { name: 'a' },
                obj2 = { name: 'b' };

            expect(list.length).to.be(0);

            list.insert(obj1);
            expect(list.length).to.be(1);

            list.insert(obj1);
            expect(list.length).to.be(1);

            list.insert(obj2);
            expect(list.length).to.be(2);

            list.remove({});
            expect(list.length).to.be(2);

            list.remove(obj1);
            expect(list.length).to.be(1);

            list.removeAll();
            expect(list.length).to.be(0);
        });
    });

    provide({});
});
