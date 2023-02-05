ymaps.modules.define(util.testfile(), [
    'util.hd'
], function (provide, utilHd) {

    var oldDPR = window.devicePixelRatio,
        newDRP,
        isDPRChanged = false;

    if (typeof window.devicePixelRatio != 'undefined') {
        window.devicePixelRatio = oldDPR + 1;

        newDRP = window.devicePixelRatio;
        isDPRChanged = newDRP == oldDPR + 1;
    }

    describe('util.hd', function () {
        var hash;

        beforeEach(function () {
            hash = {
                "1": 1,
                "1.5": 1.5,
                "2": 2,
                "2.3": 2.3,
                "3": 3
            };

            window.devicePixelRatio = 1;
        });


        it('должен корректно выбрать результат для dPR 1', function () {
            expect(utilHd.getPixelRatio()).to.be(1);
            expect(utilHd.selectValue(hash)).to.be(1);
            expect(utilHd.selectRatio(hash)).to.be(1);
        });

        // Если поле статическое, нет смысла прогонять тесты,
        // заточенные под devicePixelRatio отличный от текущего.
        if (isDPRChanged) {
            it('должен корректно выбрать результат для dPR 2', function () {
                window.devicePixelRatio = 2;

                expect(utilHd.getPixelRatio()).to.be(2);
                expect(utilHd.selectValue(hash)).to.be(2);
                expect(utilHd.selectRatio(hash)).to.be(2);
            });

            it('должен корректно выбрать результат для значения, большего чем есть в хэше — 4', function () {
                window.devicePixelRatio = 4;

                expect(utilHd.getPixelRatio()).to.be(4);
                expect(utilHd.selectValue(hash)).to.be(3);
                expect(utilHd.selectRatio(hash)).to.be(3);
            });

            it('должен корректно выбрать дробный результат для дробного DPR', function () {
                window.devicePixelRatio = 2.3;

                expect(utilHd.getPixelRatio()).to.be(2.3);
                expect(utilHd.selectValue(hash)).to.be(2.3);
                expect(utilHd.selectRatio(hash)).to.be(2.3);
            });
            
            it('должен корректно обработать ситуацию, когда хэш задается неотсортировано', function () {
                window.devicePixelRatio = 2;

                var hash = {
                    '3': 3,
                    '4': 4,
                    '2.5': 2.5
                };

                expect(utilHd.getPixelRatio()).to.be(2);
                expect(utilHd.selectValue(hash)).to.be(2.5);
                expect(utilHd.selectRatio(hash)).to.be(2.5);
            });

            it('должен корректно обработать ситуацию, когда задается неотсортированый хэш и используется больший дробный DPR', function () {
                window.devicePixelRatio = 2.65;

                var hash = {
                    '3': 3,
                    '4': 4,
                    '2.5': 2.5,
                    '2.6': 2.6,
                    '2': 2,
                    '2.7': 2.7,
                    '2.8': 2.8
                };

                expect(utilHd.getPixelRatio()).to.be(2.65);
                expect(utilHd.selectValue(hash)).to.be(2.7);
                expect(utilHd.selectRatio(hash)).to.be(2.7);
            });

            it('должен корректно обработать ситуацию, когда на вход дается не хэш, а строка', function () {
                expect(utilHd.selectValue("1")).to.be("1");
                expect(utilHd.selectRatio("5")).to.be(1);
            });
        }
    });

    provide();
});
