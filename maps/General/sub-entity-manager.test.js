ymaps.modules.define(util.testfile(), [
    "geometryEditor.component.SubEntityManager",
    "util.extend"
], function (provide, SubEntityManager, utilExtend) {
    describe("geometryEditor.component.SubEntityManager", function () {
        var changeCnt,
            foundCnt,
            // Используем объектные обертки над примитивными типами, а не обычные объекты
            // для удобства отладки.
            keys = [new String(0), new String(1), new String(2)],
            longKeys = keys.concat([new String(3), new String(4), new String(5)]);

        function checkData (subs, keys) {
            for (var i = 0, l = keys.length; i < l; i++) {
                if (
                    keys[i] !== subs[i].data ||
                    i != subs[i].index
                ) {
                    return false;
                }
            }
            return true;
        }

        function createStandardManager (keys, callbacks, params) {
            return new SubEntityManager(keys, utilExtend({
                create: {
                    callback: function (key, index) {
                        return {
                            data: key,
                            index: index,
                            setIndex: function (i) {
                                this.index = i;
                            },
                            destroy: function () {}
                        };
                    },
                    context: this
                },
                found: {
                    callback: function (object, key, i) {
                        foundCnt++;
                        object.setIndex(i);
                    },
                    context: this
                },
                change: {
                    callback: function () {
                        changeCnt++;
                    },
                    context: this
                }
            }, callbacks), utilExtend({
                keyFieldName: "data"
            }, params));
        }

        beforeEach(function () {
            changeCnt = 0;
            foundCnt = 0;
        });

        it("Состояние непосредственно после создания", function (done) {
            var manager = createStandardManager(keys),
                subs = manager.getSubEntities();

            expect(subs.length).to.be(3);
            expect(checkData(subs, keys)).to.be.ok();
            expect(changeCnt).to.be(0);
            done();
        });

        it("Задание тех же данных не приводит к вызову change callback", function (done) {
            var manager = createStandardManager(keys),
                subsOld = manager.getSubEntities();
            manager.update(keys);
            var subs = manager.getSubEntities();

            expect(subs.length).to.be(3);
            expect(checkData(subs, keys)).to.be.ok();
            expect(subs).to.be(subsOld);
            expect(changeCnt).to.be(0);
            done();
        });

        it("Задание ключей с добавленным в начало элементом", function (done) {
            var manager = createStandardManager(keys),
                subsOld = manager.getSubEntities().slice(),
                newKeys = keys.slice();
            newKeys.unshift({});
            manager.update(newKeys);
            var subs = manager.getSubEntities();

            expect(subs.length).to.be(4);
            expect(checkData(subs, newKeys)).to.be.ok();
            expect(changeCnt).to.be(1);
            expect(subs[1]).to.be(subsOld[0]);
            expect(subs[2]).to.be(subsOld[1]);
            expect(subs[3]).to.be(subsOld[2]);
            done();
        });

        it("Задание ключей с добавленным в конец элементом", function (done) {
            var manager = createStandardManager(keys),
                subsOld = manager.getSubEntities().slice(),
                newKeys = keys.slice();
            newKeys.push({});
            manager.update(newKeys);
            var subs = manager.getSubEntities();

            expect(subs.length).to.be(4);
            expect(checkData(subs, newKeys)).to.be.ok();
            expect(changeCnt).to.be(1);
            expect(subs[0]).to.be(subsOld[0]);
            expect(subs[1]).to.be(subsOld[1]);
            expect(subs[2]).to.be(subsOld[2]);
            done();
        });

        it("Задание ключей с добавленным в середину элементом", function (done) {
            var manager = createStandardManager(longKeys),
                subsOld = manager.getSubEntities().slice(),
                newKeys = longKeys.slice();
            newKeys.splice(1, 0, {});
            manager.update(newKeys);
            var subs = manager.getSubEntities();

            expect(subs.length).to.be(7);
            expect(checkData(subs, newKeys)).to.be.ok();
            expect(changeCnt).to.be(1);
            expect(subs[0]).to.be(subsOld[0]);
            expect(subs[2]).to.be(subsOld[1]);
            expect(subs[3]).to.be(subsOld[2]);
            expect(subs[4]).to.be(subsOld[3]);
            expect(subs[5]).to.be(subsOld[4]);
            expect(subs[6]).to.be(subsOld[5]);
            done();
        });

        it("Задание ключей с теми же элементами, но поменянными местами", function (done) {
            var manager = createStandardManager(longKeys),
                subsOld = manager.getSubEntities().slice(),
                newKeys = longKeys.slice();
            newKeys.splice(1, 0, newKeys.splice(2, 1)[0]);
            manager.update(newKeys);
            var subs = manager.getSubEntities();

            expect(subs.length).to.be(6);
            expect(checkData(subs, newKeys)).to.be.ok();
            expect(changeCnt).to.be(1);
            expect(subs[0]).to.be(subsOld[0]);
            expect(subs[1]).to.be(subsOld[2]);
            expect(subs[2]).to.be(subsOld[1]);
            expect(subs[3]).to.be(subsOld[3]);
            expect(subs[4]).to.be(subsOld[4]);
            expect(subs[5]).to.be(subsOld[5]);
            done();
        });

        it("Задание ключей с удаленным элементом", function (done) {
            var manager = createStandardManager(keys),
                subsOld = manager.getSubEntities().slice(),
                newKeys = keys.slice();
            newKeys.splice(1, 1);
            manager.update(newKeys);
            var subs = manager.getSubEntities();

            expect(subs.length).to.be(2);
            expect(checkData(subs, newKeys)).to.be.ok();
            expect(changeCnt).to.be(1);
            expect(subs[0]).to.be(subsOld[0]);
            expect(subs[1]).to.be(subsOld[2]);

            done();
        });

        it("Задание пустых данных при исходных пустых данных MAPSAPI-9339", function (done) {
            var manager = createStandardManager([]);
            manager.update([]);
            var subs = manager.getSubEntities();

            expect(subs.length).to.be(0);
            expect(changeCnt).to.be(0);

            done();
        });

        it("Замена элемента без reuse", function (done) {
            var manager = createStandardManager(longKeys),
                subsOld = manager.getSubEntities().slice(),
                newKeys = longKeys.slice();
            newKeys.splice(2, 1, {});
            manager.update(newKeys);
            var subs = manager.getSubEntities();

            expect(subs.length).to.be(6);
            expect(checkData(subs, newKeys)).to.be.ok();
            expect(changeCnt).to.be(1);
            expect(foundCnt).to.be(5);
            expect(subs[0]).to.be(subsOld[0]);
            expect(subs[1]).to.be(subsOld[1]);
            expect(subs[2]).to.not.be(subsOld[2]);
            expect(subs[3]).to.be(subsOld[3]);
            expect(subs[4]).to.be(subsOld[4]);
            expect(subs[5]).to.be(subsOld[5]);
            done();
        });

        it("Замена элемента с reuse", function (done) {
            var reuseCnt = 0,
                manager = createStandardManager(longKeys, {
                    reuse: {
                        callback: function (object, key, i) {
                            reuseCnt++;
                            object.data = key;
                            object.setIndex(i);
                        },
                        context: this
                    }
                }, {
                    reuse: true
                }),
                subsOld = manager.getSubEntities().slice(),
                newKeys = longKeys.slice();
            newKeys.splice(2, 1, {});
            manager.update(newKeys);
            var subs = manager.getSubEntities();

            expect(subs.length).to.be(6);
            expect(checkData(subs, newKeys)).to.be.ok();
            expect(changeCnt).to.be(1);
            expect(foundCnt).to.be(5);
            expect(reuseCnt).to.be(1);
            expect(subs[0]).to.be(subsOld[0]);
            expect(subs[1]).to.be(subsOld[1]);
            expect(subs[2]).to.be(subsOld[2]);
            expect(subs[3]).to.be(subsOld[3]);
            expect(subs[4]).to.be(subsOld[4]);
            expect(subs[5]).to.be(subsOld[5]);
            done();
        });
    });

    provide();
});
