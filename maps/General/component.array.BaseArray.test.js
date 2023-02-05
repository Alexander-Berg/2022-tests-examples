ymaps.modules.define(util.testfile(), [
    "component.array.BaseArray"
], function (provide, BaseArray) {
    describe("component.array.BaseArray", function () {
        var added,
            removed,
            seted,
            array;

        beforeEach(function () {
            added = [];
            removed = [];
            seted = [];
            array = new BaseArray([], {
                onAdd: {
                    callback: function (index, child) {
                        added.push({
                            child: child,
                            index: index
                        });
                    },
                    context: this
                },
                onRemove: {
                    callback: function (index, child) {
                        removed.push({
                            child: child,
                            index: index
                        });
                    },
                    context: this
                },
                onSet: {
                    callback: function (index, prevChild, child) {
                        seted.push({
                            prevChild: prevChild,
                            child: child,
                            index: index
                        });
                    },
                    context: this
                }
            });
        });

        it('Должен вернуть undefined, если спросить через get несуществующий элемент', function () {
            expect(array.get(3)).to.be(undefined);
        });

        it('Должен вернуть элемент, заданный в конструкторе', function () {
            var array1 = new BaseArray([1, 2, 3]);
            expect(array1.get(2)).to.be(3);
        });

        it('Должен вернуть нулевую длину пустого массива', function () {
            expect(array.getLength()).to.be(0);
        });

        it('Должен корректно добавить элемент', function () {
            array.add(1);
            var lastAdded = added.pop();
            expect(array.getLength()).to.be(1);
            expect(lastAdded.index).to.be(0);
            expect(array.get(0)).to.be(1);
        });

        it('Должен корректно добавить элемент по индексу', function () {
            array.add(1);
            array.add(2, 0);
            var lastAdded = added.pop();
            expect(array.getLength()).to.be(2);
            expect(lastAdded.index).to.be(0);
            expect(array.get(0)).to.be(2);
        });

        it('Должен корректно пережить добавление разных типов элементов', function () {
            var dataToAdd = [
                    null, 0, "", {}, 1, []
                ],
                lastAdded;
            for (var i = 0; i < dataToAdd.length; i++) {
                array.add(dataToAdd[i], i);
                lastAdded = added.pop();
                expect(array.getLength()).to.be(i + 1);
                expect(lastAdded.index).to.be(i);
                expect(array.get(i)).to.be(dataToAdd[i]);
            }
        });

        it('Должен корректно удалить элемент', function () {
            array.add(1);
            array.add(2, 0);

            array.remove(2);
            var lastRemoved = removed.pop();
            expect(array.getLength()).to.be(1);
            expect(lastRemoved.index).to.be(0);
            expect(lastRemoved.child).to.be(2);
            expect(array.get(0)).to.be(1);
        });

        it('Должен корректно выполнить removeAll для пустого массива', function () {
            array.removeAll();
            var lastRemoved = removed.pop();
            expect(lastRemoved).not.to.be.ok();
            expect(array.getLength()).to.be(0);
        });

        it('Должен корректно выполнить removeAll', function () {
            array.add(1);
            array.add(2);
            array.removeAll();
            expect(array.getLength()).to.be(0);
            expect(removed.length).to.be(2);
        });

        it('Должен корректно выполнить indexOf', function () {
            array.add(1);
            expect(array.indexOf(1)).to.be(0);
            expect(array.indexOf(2)).to.be(-1);
        });

        it('Должен корректно выполнить splice', function () {
            array.add(1);
            array.add(2);
            array.add(3);
            added = [];

            var removed = array.splice(2, 2, 4, 4);
            expect(removed.length).to.be(1);
            expect(removed[0]).to.be(3);
            expect(array.getLength()).to.be(4);
            expect(seted.length).to.be(1);
            expect(seted[0].index).to.be(2);
            expect(seted[0].prevChild).to.be(3);
            expect(added.length).to.be(1);
            expect(added[0].index).to.be(3);
        });

        it('Должен корректно выполнить splice с удалением', function () {
            array.add(1);
            array.add(2);
            array.add(3);

            var removedElements = array.splice(0, 3, 4);
            expect(removedElements.length).to.be(3);
            expect(removedElements[0]).to.be(1);
            expect(removedElements[1]).to.be(2);
            expect(removedElements[2]).to.be(3);
            expect(array.getLength()).to.be(1);
            expect(seted.length).to.be(1);
            expect(seted[0].index).to.be(0);
            expect(seted[0].prevChild).to.be(1);
            expect(removed.length).to.be(2);
            expect(removed[0].index).to.be(1);
            expect(removed[0].child).to.be(2);
            expect(removed[1].index).to.be(1);
            expect(removed[1].child).to.be(3);
        });

        it('Должен корректно отработать each', function () {
            var context = this,
                func = function (o, i) {
                    str += o + "," + i + "," + (this == context) + ";";
                },
                str;
            array.add(1);
            array.add(2);

            str = "";
            array.each(func);
            expect(str).to.be('1,0,false;2,1,false;');

            str = "";
            array.each(func, this);
            expect(str).to.be('1,0,true;2,1,true;');
        });

        it('Должен проитерировать через iterator', function () {
            array.add(1);
            array.add(2);

            var iterator = array.getIterator();
            expect(iterator.getNext()).to.be(1);
            expect(iterator.getNext()).to.be(2);
            expect(iterator.getNext()).to.be(iterator.STOP_ITERATION);
        });

        it('Должен проитерировать через iterator с удалением в процессе', function () {
            array.add(1);
            array.add(2);
            array.add(3);
            array.add(4);

            var iterator = array.getIterator();
            iterator.getNext();
            array.remove(1);
            iterator.getNext();

            expect(iterator.getNext()).to.be(4);
            expect(iterator.getNext()).to.be(iterator.STOP_ITERATION);
        });
    });

    provide();
});
