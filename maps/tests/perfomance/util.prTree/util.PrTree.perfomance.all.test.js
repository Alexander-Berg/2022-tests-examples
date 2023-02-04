ymaps.modules.define('perfomance.util.PrTree.all', [
    "util.PrTree"
], function (provide, Tree) {

    var W = 1000,
        N = 10000,
        TN = 1000,
        items = generateRectangles(N, W),
        bboxes = {
            '1k40': generateRectangles(TN, W * Math.sqrt(0.4)),
            '1k20': generateRectangles(TN, W * Math.sqrt(0.2)),
            '1k10': generateRectangles(TN, W * Math.sqrt(0.1)),
            '1k1': generateRectangles(TN, 10),
            '1k0.01': generateRectangles(TN, 1)
        };

    describe('Тесты на производительность PR деревьев', function () {
        before(function () {
            console.group('PR TREE: PERFOMANCE');
        });

        after(function () {
            console.groupEnd('PR TREE: PERFOMANCE');
        });

        runTest(500);
        runTest(1000);
        runTest(5000);
        runTest(10000);
        runTest(25000);
        runTest(50000);
        runTest(100000);
    });

    function runTest (numElements) {
        describe(numElements + ' objects', function () {
            var N = numElements,
                items = generateRectangles(N, W);
            before(function () {
                console.groupCollapsed(numElements.toString());
            });
            after(function () {
                console.groupEnd(numElements.toString());
            });

            describe('One by one', function () {
                before(function () {
                    console.group('ONE BY ONE');
                    console.log('WORK WITH ' + N + " ITEMS");
                });

                after(function () {
                    console.groupEnd('ONE BY ONE');
                });

                it('insert', function () {
                    var tree = new Tree();

                    var timeTitle = 'insert';

                    console.time(timeTitle);
                    for (var i = 0; i < N; i++) {
                        tree.insert(items[i]);
                    }
                    console.timeEnd(timeTitle);
                });

                it('remove', function () {
                    var tree = new Tree();
                    for (var i = 0; i < N; i++) {
                        tree.insert(items[i]);
                    }

                    var timeTitle = 'remove';

                    console.time(timeTitle);
                    for (var j = 0; j < TN; j++) {
                        tree.remove(items[j]);
                    }
                    console.timeEnd(timeTitle);
                });

                describe('search', function () {
                    var tree = new Tree();
                    for (var i = 0; i < N; i++) {
                        tree.insert(items[i]);
                    }

                    runSearchCases(tree, items);
                });
            });

            describe('Bulk load', function () {
                before(function () {
                    console.group('BULK LOAD');
                    console.log('WORK WITH ' + N + " ITEMS");
                });

                after(function () {
                    console.groupEnd('BULK LOAD');
                });

                it('insert', function () {
                    var tree = new Tree();

                    var timeTitle = 'insert';

                    console.time(timeTitle);
                    tree.insert(items);
                    console.timeEnd(timeTitle);
                });

                it('remove', function () {
                    var tree = new Tree();
                    tree.insert(items);

                    var timeTitle = 'remove';

                    console.time(timeTitle);
                    for (var j = 0; j < TN; j++) {
                        tree.remove(items[j]);
                    }
                    console.timeEnd(timeTitle);
                });

                describe('search', function () {
                    var tree = new Tree();
                    tree.insert(items);

                    runSearchCases(tree);
                });
            });
        });
    }

    function runSearchCases (tree) {
        describe('search', function () {
            before(function () {
                console.group('search');
            });

            after(function (){
                console.groupEnd('search');
            });

            describe("1 search", function () {
                before(function () {
                    console.groupCollapsed('1 search');
                });

                after(function () {
                    console.groupEnd('1 search');
                });

                it('40%', function () {
                    var timesTitle = '40%';
                    console.time(timesTitle);
                    tree.search(bboxes['1k40'][10].bbox);
                    console.timeEnd(timesTitle);

                });

                it('20%', function () {
                    var timesTitle = '20%';
                    console.time(timesTitle);
                    tree.search(bboxes['1k20'][10].bbox);
                    console.timeEnd(timesTitle);

                });

                it('10%', function () {
                    var timesTitle = '10%';
                    console.time(timesTitle);
                    tree.search(bboxes['1k10'][10].bbox);
                    console.timeEnd(timesTitle);

                });

                it('1%', function () {
                    var timesTitle = '1%';
                    console.time(timesTitle);
                    tree.search(bboxes['1k1'][10].bbox);
                    console.timeEnd(timesTitle);

                });

                it('0.01%', function () {
                    var timesTitle = '0.01%';
                    console.time(timesTitle);
                    tree.search(bboxes['1k0.01'][10].bbox);
                    console.timeEnd(timesTitle);
                });
            });

            describe('1000 searches', function () {

                before(function () {
                    console.group('1000 searches');
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