ymaps.modules.define(util.testfile(), [
    "option.manager.Monitor",
    "option.Manager"
], function (provide, Monitor, OptionManager) {
    describe("option.manager.Monitor", function () {
        var optionManager,
            defaultValues = {
                a: -1,
                b: -2,
                c: -3,
                d: -4
            };

        function failCallback () {
            expect().fail("Wrong change callback call");
        }

        beforeEach(function () {
            optionManager = new OptionManager({
                a: 1,
                b: 2,
                c: 3
            });
        });

        it("Возвращаемые значения при добавлении единичного слушателя", function (done) {
            var monitor = new Monitor(optionManager);

            monitor.add("a", failCallback);
            monitor.add("d", failCallback);

            expect(monitor.get("a")).to.be(1);
            // undefined, т.к. слушатель не добавлялся.
            expect(monitor.get("b")).to.be.an("undefined");
            // Передача defaultValues не должна влиять на результат, т.к. слушатель не добавлялся.
            expect(monitor.get("b", defaultValues)).to.be.an("undefined");
            // Слушатель добавлялся, но значение не определено.
            expect(monitor.get("d")).to.be.an("undefined");
            // Передача defaultValues влияет на результат.
            expect(monitor.get("d", defaultValues)).to.be(-4);

            done();
        });

        it("Возвращаемые значения при добавлении множественного слушателя", function (done) {
            var monitor = new Monitor(optionManager);

            monitor.add(["a", "b", "d"], failCallback);

            expect(monitor.get("a")).to.be(1);
            expect(monitor.get("b")).to.be(2);
            expect(monitor.get("c", defaultValues)).to.be.an("undefined");
            expect(monitor.get("d", defaultValues)).to.be(-4);

            done();
        });

        it("Возвращаемые значения после удаления последнего слушателя", function (done) {
            var monitor = new Monitor(optionManager);

            monitor.add("a", failCallback);
            monitor.remove("a", failCallback);

            expect(monitor.get("a")).to.be.an("undefined");
            expect(monitor.get("a", defaultValues)).to.be.an("undefined");

            done();
        });

        it("Возвращаемые значения после удаления не последнего слушателя", function (done) {
            var monitor = new Monitor(optionManager);

            monitor.add("a", failCallback);
            monitor.add(["a", "b"], failCallback);
            monitor.remove(["a", "b"], failCallback);

            expect(monitor.get("a")).to.be(1);
            expect(monitor.get("b")).to.be.an("undefined");

            done();
        });

        it("Изменение значений для единичных слушателей", function (done) {
            var monitor = new Monitor(optionManager);

            monitor
                .add("a", function (newValue, oldValue) {
                    expect(newValue).to.be.an("undefined");
                    expect(oldValue).to.be(1);
                    expect(monitor.get("a")).to.be.an("undefined");
                    expect(monitor.get("a", defaultValues)).to.be(-1);
                })
                .add("b", failCallback)
                .add("c", failCallback, null, defaultValues)
                .add("d", function (newValue, oldValue) {
                    expect(newValue).to.be(14);
                    // oldValue для d равно -4, т.к. при подписке был передан defaultValues.
                    expect(oldValue).to.be(-4);
                    // get как с передачей defaultValues, так и без, должен давать одинаковый
                    // результат, т.к. d определено.
                    expect(monitor.get("d")).to.be(14);
                    expect(monitor.get("d", defaultValues)).to.be(14);
                    done();
                }, null, defaultValues);

            optionManager.unset("a");
            optionManager.set("d", 14);
            monitor.checkChange();
        });

        it("Изменение значений для множественных слушателей", function (done) {
            var monitor = new Monitor(optionManager);

            monitor
                .add(["a", "c"], function (newValues, oldValues) {
                    expect(newValues).to.eql({
                        a: -1,
                        c: -3
                    });
                    expect(oldValues).to.eql({
                        a: 1,
                        c: 3
                    });
                    expect(monitor.get("a")).to.be(-1);
                    expect(monitor.get("c")).to.be(-3);
                })
                .add(["b", "c"], function (newValues, oldValues) {
                    expect(newValues).to.eql({
                        b: 2,
                        c: -3
                    });
                    expect(oldValues).to.eql({
                        b: 2,
                        c: 3
                    });
                    expect(monitor.get("b")).to.be(2);
                    expect(monitor.get("c")).to.be(-3);
                    done();
                });

            optionManager.set({
                a: -1,
                c: -3
            });
            monitor.checkChange();
        });

        it("Проверка контекста вызова обработчиков", function (done) {
            var monitor = new Monitor(optionManager),
                callbackContext = {};

            monitor
                .add(["a", "c"], function () {
                    expect(this).to.be(callbackContext);
                }, callbackContext)
                .add("b", function () {
                    expect(this).to.be(callbackContext);
                }, callbackContext)
                .add(["a", "c"], function () {
                    expect(this).to.be(window);
                })
                .add("b", function () {
                    expect(this).to.be(window);
                    done();
                });

            optionManager.set({
                a: -1,
                b: -2
            });
            monitor.checkChange();
        });

        it("Задание значения по умолчанию должно учитываться при сравнении старых и новых значений внутри монитора", function (done) {
            var monitor = new Monitor(optionManager);

            monitor.add("d", failCallback, null, defaultValues);

            // Обработчик изменения не должен быть вызван, т.к. d не изменялся.
            optionManager.set({
                a: -1
            });
            monitor.checkChange();

            optionManager.set({
                d: -4
            });
            monitor.checkChange();

            done();
        });
    });

    provide();
});
