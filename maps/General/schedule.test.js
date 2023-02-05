ymaps.modules.define(util.testfile(), [
    'util.scheduler.manager',
    'util.scheduler.Group',
    // strategies
    'util.scheduler.strategy.Asap',
    'util.scheduler.strategy.Background',
    'util.scheduler.strategy.Now',
    'util.scheduler.strategy.Processing'
], function (provide, manager, schGroup) {
    describe('util.nodeSize', function () {
        this.timeout(10000);

        var schedule = manager.schedule;
        var unschedule = manager.unschedule;

        var startTime = 0;
        var endTime = 0;
        var hasError = false;

        beforeEach(function () {
            startTime = 0;
            endTime = 0;
            hasError = false;
        });

        function scheduleCallback() {
            endTime = +(new Date());
        }

        function runScheduleTest(name, params) {
            startTime = +(new Date());
            schedule(name, scheduleCallback, null, params);
        }

        function scheduleError() {
            hasError = true;
            setTimeout(function () {
                throw new Error('Не должно было выполниться');
            }, 1);
        }

        describe('Базовые команды', function () {
            it('Должен отработать в 100мс', function (done) {
                runScheduleTest('timeout', 100);
                setTimeout(function () {
                    var deltaTime = endTime - startTime;
                    expect(deltaTime).to.be.between(100, 200);
                    done();
                }, 1000);
            });

            it('Стратегия Now (исполнение на месте)', function (done) {
                runScheduleTest('now');
                setTimeout(function () {
                    var deltaTime = endTime - startTime;
                    expect(deltaTime).to.be.between(0, 10);
                    done();
                }, 200);
            });

            it('Стратегия Asap (исполнение максимально быстро)', function (done) {
                runScheduleTest('asap', 1);
                setTimeout(function () {
                    var deltaTime = endTime - startTime;
                    // Depends highly on frame rate and business of micro-task queue.
                    expect(deltaTime).to.be.between(0, 40);
                    done();
                }, 200);
            });

            it('Действие должно быть отменено. Cтратегия Asap', function (done) {
                schedule('asap', scheduleError, null, 1);
                unschedule('asap', scheduleError, null);
                setTimeout(function () {
                    expect(hasError).to.be(false);
                    done();
                }, 100);
            });

            it('Стратегия Background (исполнение в фоном режиме)', function (done) {
                runScheduleTest('background', 1);
                setTimeout(function () {
                    var deltaTime = endTime - startTime;
                    expect(deltaTime).to.be.between(0, 100);
                    done();
                }, 200);
            });
        });

        describe('Тесты замыканий', function () {
            it('Unbind', function (done) {
                schedule('timeout', scheduleError, null, 1);
                unschedule('timeout', scheduleError, null);
                schedule('timeout', scheduleError, this, 1);
                unschedule('timeout', scheduleError, this);
                schedule('timeout', scheduleCallback, this, 1);
                unschedule('timeout', scheduleCallback, {});
                setTimeout(function () {
                    expect(hasError).to.be(false);
                    expect(endTime).not.to.be(0);
                    done();
                }, 100);
            });
        });

        describe('Группы', function () {
            it('Должен отработать нормально scheduler.Group', function (done) {
                var group = new schGroup();
                group.schedule('asap', scheduleCallback);
                setTimeout(function () {
                    expect(endTime).not.to.be(0);
                    done();
                }, 200);
            });

            it('Должен отработать групповой unbind', function (done) {
                var pass = 0;
                var group = new schGroup();

                group.schedule('timeout', function () {
                    pass++;
                }, null, 10);
                group.schedule('timeout', function () {
                    pass++;
                }, null, 10);
                group.unschedule('timeout', scheduleError);

                setTimeout(function () {
                    expect(hasError).to.be(false);
                    expect(pass).to.be(2);
                    done();
                }, 200);
            });

            it('Должен отработать групповой removeAll', function (done) {
                var group = new schGroup();
                var errors = [];

                group.schedule('timeout', function () {
                    errors.push(10);
                }, null, 10);
                group.schedule('asap', function () {
                    errors.push('asap');
                });
                group.schedule('background', function () {
                    errors.push('background');
                });
                group.removeAll();

                setTimeout(function () {
                    expect(errors.length).to.be(0);
                    done();
                }, 200);
            });
        });

        describe('Анимации', function () {
            it('Processesing', function (done) {
                var ticks = 0;
                var succ = false;
                schedule('processing', function () {
                    ticks++;
                }, null, {
                        onComplete: function () {
                            succ = true;
                        },
                        duration: 1000
                    });

                setTimeout(function () {
                    expect(ticks).to.be.greaterThan(0);
                    expect(succ).to.be(true);
                    done();
                }, 1200);
            });

            it('Processesing Abort', function (done) {
                var ticks = 0;
                var succ = false;
                schedule('processing',
                    function () {
                        ticks++;
                    }, null, {
                        onComplete: function () {
                            succ = true;
                        },
                        duration: 1000
                    }).stop();


                setTimeout(function () {
                    expect(ticks).to.be(0);
                    expect(succ).to.be(false);
                    done();
                }, 100);
            });
        });
    });
    provide();
});
