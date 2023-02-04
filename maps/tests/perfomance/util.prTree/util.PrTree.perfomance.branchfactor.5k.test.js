ymaps.modules.define('test.util.PrTree.perfomance.branchfactor.5k', [
    'util.PrTree'
], function (provide, Tree) {

    var N = 1000,
        W = 1000,
        TN = 1000,
        items = generateRectangles(N, W),
        bboxes = {
            '1k40': generateRectangles(TN, W * Math.sqrt(0.4)),
            '1k20': generateRectangles(TN, W * Math.sqrt(0.2)),
            '1k10': generateRectangles(TN, W * Math.sqrt(0.1)),
            '1k1': generateRectangles(TN, 10),
            '1k0.01': generateRectangles(TN, 1)
        };

    describe("util.PrTree Скорость пострения дерева с разными значениями branchfactor", function () {
        before(function () {
            console.group('PR TREE: BRANCHFACTORS');
        });

        after(function () {
            console.groupEnd('PR TREE: BRANCHFACTORS');
        });

        // Тестирование производительности деревьев с различным максимальным
        // количеством элементов в ноде: от 2 до 20.
        for (var i = 2; i < 20; i++) {
            runTest(i);
        }
    });

    function runTest (branchFactor) {
        var branchFactorTitle = 'BRANCHFACTOR: ' + branchFactor.toString();

        describe(branchFactorTitle, function () {
            before(function () {
                console.groupCollapsed(branchFactorTitle);
            });

            after(function () {
                console.groupEnd(branchFactorTitle);
            });

            describe('ONE BY ONE', function () {
                before(function () {
                    console.group('ONE BY ONE');
                });

                after(function () {
                    console.groupEnd('ONE BY ONE');
                });

                it('insert', function () {
                    var tree = new Tree(branchFactor);
                    var title = 'insert';

                    console.time(title);
                    for (var i = 0; i < N; i++) {
                        tree.insert(items[i]);
                    }
                    console.timeEnd(title);
                });

                it('remove', function () {
                    var tree = new Tree(branchFactor);
                    var title = 'remove';

                    for (var i = 0; i < N; i++) {
                        tree.insert(items[i]);
                    }

                    console.time(title);
                    for (var i = 0; i < N; i++) {
                        tree.remove(items[i]);
                    }
                    console.timeEnd(title);
                });

                var searchTree = new Tree(branchFactor);
                for (var i = 0; i < N; i++) {
                    searchTree.insert(items[i]);
                }
                runSearchBranchFactorCase(searchTree);
            });

            describe('BULK', function () {
                before(function () {
                    console.group('BULK');
                });

                after(function () {
                    console.groupEnd('BULK');
                });

                it('insert', function () {
                    var tree = new Tree(branchFactor);
                    var title = 'insert';

                    console.time(title);
                    tree.insert(items);
                    console.timeEnd(title);
                });

                it('remove', function () {
                    var tree = new Tree(branchFactor);
                    var title = 'remove';

                    tree.insert(items);

                    console.time(title);
                    for (var i = 0; i < N; i++) {
                        tree.remove(items[i]);
                    }
                    console.timeEnd(title);
                });

                runSearchBranchFactorCase(new Tree(branchFactor, items));
            });
        });
    }

    function runSearchBranchFactorCase (tree) {
        describe('1000 searches', function () {
            before(function () {
                console.groupCollapsed('1000 searches');
            });

            after(function () {
                console.groupEnd('1000 searches');
            });

            it('40%', function () {
                var boxes = bboxes['1k40'];
                var timesTitle = '40%';
                console.time(timesTitle);
                for (var i = 0; i < TN; i++) {
                    tree.search(boxes[i].bbox);
                }
                console.timeEnd(timesTitle);
            });

            it('20%', function () {
                var boxes = bboxes['1k20'];
                var timesTitle = '20%';
                console.time(timesTitle);
                for (var i = 0; i < TN; i++) {
                    tree.search(boxes[i].bbox);
                }
                console.timeEnd(timesTitle);
            });

            it('10%', function () {
                var boxes = bboxes['1k10'];
                var timesTitle = '10%';
                console.time(timesTitle);
                for (var i = 0; i < TN; i++) {
                    tree.search(boxes[i].bbox);
                }
                console.timeEnd(timesTitle);
            });

            it('1%', function () {
                var boxes = bboxes['1k1'];
                var timesTitle = '1%';
                console.time(timesTitle);
                for (var i = 0; i < TN; i++) {
                    tree.search(boxes[i].bbox);
                }
                console.timeEnd(timesTitle);
            });

            it('0.01%', function () {
                var boxes = bboxes['1k0.01'];
                var timesTitle = '0.01%';
                console.time(timesTitle);
                for (var i = 0; i < TN; i++) {
                    tree.search(boxes[i].bbox);
                }
                console.timeEnd(timesTitle);
            });
        });
    }

    function getRandomRectangle (size, width) {
        var x = Math.random() * (width - size),
            y = Math.random() * (width - size);

        return {bbox: [
            [x, y],
            [x + width * Math.random(), y + width * Math.random()]
        ]};
    }

    function generateRectangles (num, width) {
        var data = [];
        for (var i = 0; i < num; i++) {
            data[i] = getRandomRectangle(1, width);
        }
        return data;
    }

    provide();
});