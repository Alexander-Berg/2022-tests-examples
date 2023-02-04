ymaps.modules.define('perfomance.util.PrTree.5k', [
    "util.PrTree"
], function (provide, Tree) {

    var W = 1000,
        items;

    describe('Тесты на производительность PR деревьев 5k objects', function () {
        before(function () {
            console.group('PR TREE: CREATE TIME');
        });

        after(function () {
            console.groupEnd('PR TREE: CREATE TIME');
        });

        runTestCase(500);
        runTestCase(1000);
        runTestCase(5000);
    });

    function runTestCase (elementsNumber) {
        var title = elementsNumber.toString() + ' elements';
        describe(title, function () {
            before(function () {
                items = generateRectangles(elementsNumber, W);
                console.group(title);
            });

            after(function () {
                console.groupEnd(title);
            });

            it('insert', function () {
                console.time('1k times insert');
                var tree = new Tree();
                for (var i = 0; i < 1000; i++) {
                    for (var j = 0, k = items.length; j < k; j++) {
                        tree.insert(items[j]);
                    }
                    tree.removeAll();
                }
                console.timeEnd('1k times insert');
            });

            it('bulk-load', function () {
                console.time('1k times bulk-load');
                var tree = new Tree();
                for (var i = 0; i < 1000; i++) {
                    tree.insert(items);
                }
                console.timeEnd('1k times bulk-load');
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
