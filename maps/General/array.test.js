ymaps.modules.define(util.testfile(), [
    'util.array'
], function (provide, utilArray) {
    describe('util.array', function () {

        describe('#indexOf', function () {
            var x;
            var testArray
            beforeEach(function () {
                x = {};
                testArray = [1, "2", {}, x, 1];
            });

            it('Должен найти индекс числа в массиве', function () {
                var index = utilArray.indexOf(testArray, 1);
                expect(index).to.be(0);
            });

            it('Должен найти индекс строки в массиве', function () {
                var index = utilArray.indexOf(testArray, "2");
                expect(index).to.be(1);
            });

            it('Должен найти индекс переменной в массиве', function () {
                var index = utilArray.indexOf(testArray, x);
                expect(index).to.be(3);
            });

            it('Должен найти индекс переменной в массиве', function () {
                var index = utilArray.indexOf(testArray, x);
                expect(index).to.be(3);
            });

            it('Должен найти индекс числа в массиве, с учетом начальной позиции', function () {
                var index = utilArray.indexOf(testArray, 1, 2);
                expect(index).to.be(4);
            });

            it('Должен найти индекс числа в массиве, с учетом начальной отрицательной позиции', function () {
                var index = utilArray.indexOf(testArray, 1, -1);
                expect(index).to.be(4);
            });

            it('Должен вернуть -1 при поиске отсутствующего элемента в массиве', function () {
                var index = utilArray.indexOf(testArray, 2);
                expect(index).to.be(-1);
            });
        });

        describe('#each', function () {
            var testArray;
            var testObject;
            beforeEach(function () {
                testArray = [1, 2, 3, 4, 5];
                testObject = { a: 1, b: 2, c: 3, d: 4, e: 5 };
            });

            it('Должен проитерироваться по массиву', function () {
                var result = 0;
                utilArray.each(testArray, function (item) {
                    result += item;
                });

                expect(result).to.be(15);
            });

            it('Должен проитерироваться по массиву с учетом контекста', function () {
                var object = {
                    result: 0
                };

                utilArray.each(testArray, function (item) {
                    this.result += item;
                }, object);

                expect(object.result).to.be(15);
            });

            it('Должен проитерироваться по объекту', function () {
                var result = 0;
                utilArray.each(testObject, function (item) {
                    result += item;
                });

                expect(result).to.be(15);
            });

            it('Должен проитерироваться по объекту с учетом контекста', function () {
                var object = {
                    result: 0
                };

                utilArray.each(testObject, function (item) {
                    this.result += item;
                }, object);

                expect(object.result).to.be(15);
            });
        });

        describe('#map', function () {
            var testArray;
            beforeEach(function () {
                testArray = [1, 2, 3, 4, 5];
            });

            it('Должен вернуть новый массив, обработанный callback', function () {
                var newArray = utilArray.map(testArray, function (item) {
                    return item * 2;
                });

                expect(newArray).to.eql([2, 4, 6, 8, 10]);
            });

            it('Должен при мапе передать в callback элемент, индекс и исходный массив', function () {
                var testArray = [5];
                utilArray.map(testArray, function (item, index, array) {
                    expect(item).to.be(5);
                    expect(index).to.be(0);
                    expect(array).to.eql(testArray);

                    return item;
                });
            });

            it('Должен корректно отработать callback, при вызове с контекстом', function () {
                var object = {
                    value: 2
                };

                var newArray = utilArray.map(testArray, function (item) {
                    return item * this.value;
                }, object);

                expect(newArray).to.eql([2, 4, 6, 8, 10]);
            });
        });

        describe('#findIndex', function () {
            var testArray;
            beforeEach(function () {
                testArray = [1, 2, 3, 4, 5];
            });

            it('Должен произвести поиск по массиву и вернуть индекс найденного элемента', function () {
                var index = utilArray.findIndex(testArray, function (item) {
                    return item / 2 == 2;
                });

                expect(index).to.be(3);
            });

            it('Должен произвести поиск по массиву и вернуть -1', function () {
                var index = utilArray.findIndex(testArray, function (item) {
                    return item / 2 == 0;
                });

                expect(index).to.be(-1);
            });

            it('Должен произвести поиск по массиву и вернуть только первый индекс', function () {
                var index = utilArray.findIndex(testArray, function (item) {
                    var value = item / 2;
                    return value == 1 || value == 2;
                });

                expect(index).to.be(1);
            });

            it('Должен произвести поиск по массиву передать в callback элемент, индекс и исходный массив', function () {
                var index = utilArray.findIndex(testArray, function (item, index, array) {

                    expect(item).to.be(1);
                    expect(index).to.be(0);
                    expect(array).to.eql(testArray);

                    return item == 1;
                });
            });

            it('Должен произвести поиск, вызывая callback в необходимом контексте', function () {
                var object = {
                    value: 4
                };

                var index = utilArray.findIndex(testArray, function (item) {
                    return this.value == item;
                }, object);

                expect(index).to.be(3);
            });
        });

        describe('#quickSort', function () {
            var testArray;
            beforeEach(function () {
                testArray = [
                    { "text": "Владивосток", "index": 0 },
                    { "text": "Москва", "index": 1 },
                    { "text": "Екатеринбург", "index": 2 },
                    { "text": "Тюмень", "index": 3 },
                    { "text": "Алматы", "index": 4 },
                    { "text": "Санкт-Петербург", "index": 5 },
                    { "text": "Астана", "index": 6 },
                    { "text": "Барнаул", "index": 7 },
                    { "text": "Казань", "index": 8 },
                    { "text": "Краснодар", "index": 9 },
                    { "text": "Красноярск", "index": 10 },
                    { "text": "Новосибирск", "index": 11 },
                    { "text": "Омск", "index": 12 },
                    { "text": "Пермь", "index": 13 },
                    { "text": "Самара", "index": 14 }
                ];
            });

            it('Должен произвести сортировку, оставив все элементы на своих местах', function () {
                var sorted = utilArray.quickSort(testArray, function () {
                    return 0;
                });

                for (var i = 0, k = sorted.length; i < k; i++) {
                    expect(sorted[i]).to.be(testArray[i]);
                }
            });

            it('Должен произвести сортировку в обратном порядке', function () {
                var sorted = utilArray.quickSort(testArray, function (a, b) {
                    return b.index - a.index;
                });

                for (var i = 0, k = sorted.length; i < k; i++) {
                    expect(sorted[i]).to.be(testArray[k - i - 1]);
                }
            });
        });
        
        describe('#findAfterValue', function () {
            var arr = [1, 2, 3, 4, 5, 5, 6].map(function (val) {return [val, 0]});
            it('Должен найти пороговый элемент в центре массива', function () {
                expect(utilArray.findAfterValue(arr, 4)).to.be(3);
            });
            
            it('Должен корректно обработать случай, когда все элементы меньше заданного', function () {
                expect(utilArray.findAfterValue(arr, 7)).to.be(arr.length);
            });
            
            it('Должен корректно обработать случай, когда все элементы больше заданного', function () {
                expect(utilArray.findAfterValue(arr, 0)).to.be(0);
            });
            
            it('Должен корректно обработать случай, когда центр разбиения приходится на правую границу', function () {
                var arr1 = [5, 5, 5, 5, 5, 5, 6, 6, 6, 6].map(function (val) {return [val, 0]});
                expect(utilArray.findAfterValue(arr1, 6)).to.be(6);
            });
            
            it('Должен корректно обработать случай, центр разбиения приходится на левую границу', function () {
                var arr1 = [1, 1, 1, 2, 2].map(function (val) {return [val, 0]});
                expect(utilArray.findAfterValue(arr1, 1.001)).to.be(3);
            });

            it('Должен корректно обработать длинный массив близких по значению величин', function () {
                var arr1 = [];
                for (var i = 0; i < 50000; i++) {
                    arr1.push([5, 0]);
                }
                for (var i = 0; i < 50000; i++) {
                    arr1.push([6, 0]);
                }
                expect(utilArray.findAfterValue(arr1, 6)).to.be(50000);
            });
            
            it('Должен корректно отработать на пустом массиве', function () {
                expect(utilArray.findAfterValue([], 6)).to.be(0);
            });
        });
    });
    provide();
});
