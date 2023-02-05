ymaps.modules.define(util.testfile(), [
    'util.Chunker',
    'expect'
], function (provide, Chunker) {
    describe('test.util.Chunker', function () {

        var testData = [
            'One', 'Two', 'Three', 'Four', 'Five',
            'Six', 'Seven', 'Eight', 'Nine', 'Ten',
            'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen'
        ];

        it('Должен корректно обработать очередь из 15 элементов по 5 в чанке', function (done) {
            var elements = 0,
                chunker = new Chunker(testData, {
                    timeout: 0,
                    chunkSize: 5,
                    worker: function (arrayElement) {
                        elements++;
                    }
                });

            chunker.start(function () {
                expect(elements).to.be(15);
                done();
            });
        });

        it('Должен обработать очередь из 15 элементов в 1 чанке (size=30)', function (done) {
            var elements = 0,
                chunker = new Chunker(testData, {
                    timeout: 0,
                    chunkSize: 30,
                    worker: function (arrayElement) {
                        elements++;
                    }
                });

            chunker.start(function () {
                expect(elements).to.be(15);
                done();
            });
        });

        it('Должен корректно обработать очередь из 15 элементов с помощью RAF стратегии', function (done) {
            var elements = 0,
                chunker = new Chunker(testData, {
                    useRaf: true,
                    chunkSize: 5,
                    worker: function (arrayElement) {
                        elements++;
                    }
                });

            chunker.start(function () {
                expect(elements).to.be(15);
                done();
            });
        });

        it('Должен остановить обработку очереди (при options.timeout = 50), не успев обработать все элементы', function (done) {
            var elements = 0,
                chunker = new Chunker(testData, {
                    timeout: 50,
                    chunkSize: 5,
                    worker: function (arrayElement) {
                        elements++;
                    }
                });

            chunker.start();

            setTimeout(function () {
                chunker.stop();
                expect(elements).not.to.be(15);
                done();
            }, 70);
        });

        it('Должен остановить обработку очереди при использовании RAF (arr.length = 15, chunkSize = 3)', function (done) {
            var elements = 0,
                chunker = new Chunker(testData, {
                    useRaf: true,
                    chunkSize: 3,
                    worker: function (arrayElement) {
                        elements++;
                    }
                });

            chunker.start();

            setTimeout(function () {
                // Первый чанк запускается сразу же при запуске .start();
                // Второй должен запуститься сразу за ним. Это подразумевается таймаутом.
                // В итоге чанкер должен остановиться перед третьим чанком,
                // если даже не так, то в любом случае не должен успеть дойти до конца.
                chunker.stop();
                expect(elements).not.to.be(15);
                done();
            }, 1);
        });
    });

    provide();
});
