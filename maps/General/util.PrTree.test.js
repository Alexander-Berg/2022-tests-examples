ymaps.modules.define(util.testfile(), [
    "util.PrTree",
    "util.array"
], function (provide, PrTree, utilArray) {

    describe("util.PrTree", function () {
        var data = [
            {bbox: [[0,0],[0,0]], id: 1}, {bbox: [[10,10],[10,10]], id: 2},
            {bbox: [[20,20],[20,20]], id: 3}, {bbox: [[25,0],[25,0]], id: 4},
            {bbox: [[35,10],[35,10]], id: 5}, {bbox: [[45,20],[45,20]], id: 6},
            {bbox: [[0,25],[0,25]], id: 7}, {bbox: [[10,35],[10,35]], id: 8},
            {bbox: [[20,45],[20,45]], id: 9}, {bbox: [[25,25],[25,25]], id: 10},
            {bbox: [[35,35],[35,35]], id: 11}, {bbox: [[45,45],[45,45]], id: 12},
            {bbox: [[50,0],[50,0]], id: 13}, {bbox: [[60,10],[60,10]], id: 14},
            {bbox: [[70,20],[70,20]], id: 15}, {bbox: [[75,0],[75,0]], id: 16},
            {bbox: [[85,10],[85,10]], id: 17}, {bbox: [[95,20],[95,20]], id: 18},
            {bbox: [[50,25],[50,25]], id: 19}, {bbox: [[60,35],[60,35]], id: 20},
            {bbox: [[70,45],[70,45]], id: 21}, {bbox: [[75,25],[75,25]], id: 22},
            {bbox: [[85,35],[85,35]], id: 23}, {bbox: [[95,45],[95,45]], id: 24},
            {bbox: [[0,50],[0,50]], id: 25}, {bbox: [[10,60],[10,60]], id: 26},
            {bbox: [[20,70],[20,70]], id: 27}, {bbox: [[25,50],[25,50]], id: 28},
            {bbox: [[35,60],[35,60]], id: 29}, {bbox: [[45,70],[45,70]], id: 30},
            {bbox: [[0,75],[0,75]], id: 31}, {bbox: [[10,85],[10,85]], id: 32},
            {bbox: [[20,95],[20,95]], id: 33}, {bbox: [[25,75],[25,75]], id: 34},
            {bbox: [[35,85],[35,85]], id: 35}, {bbox: [[45,95],[45,95]], id: 36},
            {bbox: [[50,50],[50,50]], id: 37}, {bbox: [[60,60],[60,60]], id: 38},
            {bbox: [[70,70],[70,70]], id: 39}, {bbox: [[75,50],[75,50]], id: 40},
            {bbox: [[85,60],[85,60]], id: 41}, {bbox: [[95,70],[95,70]], id: 42},
            {bbox: [[50,75],[50,75]], id: 43}, {bbox: [[60,85],[60,85]], id: 44},
            {bbox: [[70,95],[70,95]], id: 45}, {bbox: [[75,75],[75,75]], id: 46},
            {bbox: [[85,85],[85,85]], id: 47}, {bbox: [[95,95],[95,95]], id: 48}
        ];

        describe('ONE-BY-ONE', function () {
            var tree;
            beforeEach(function () {
                tree = new PrTree();
                for (var i = 0; i < data.length; i++) {
                    tree.insert(data[i]);
                }
            });

            it ('Должен корректно вставить объекты', function () {
                tree = new PrTree();
                for (var i = 0; i < data.length; i++) {
                    tree.insert(data[i]);
                }

                var treeItems = tree.getAll();
                for (var i = 0; i < data.length; i++) {
                    if (utilArray.indexOf(treeItems, data[i]) == -1) {
                        throw new Error('Элемент не вставился в дерево.');
                    }
                }
            });

            it('Должен произвести поиск и вернуть 6 элементов', function () {
                expect(tree.search([[0, 0], [25, 25]]).length).to.be(6);
            });

            it('Должен корректно удалить половину добавленных элементов', function () {
                var dataCount = data.length,
                    removeDataCount = Math.floor(dataCount / 2),
                    i;

                for (i = 0; i < removeDataCount; i++) {
                    tree.remove(data[i]);
                }

                var allItems = tree.getAll();
                for (i = 0; i < removeDataCount; i++) {
                    if (utilArray.indexOf(allItems, data[i]) != -1) {
                        throw new Error('Элемент не удалился из дерева');
                    }
                }

                expect(allItems.length).to.be(dataCount - removeDataCount);
            });

            it('Должен вернуть все листья дерева', function () {
                expect(tree.getAll().length).to.be(data.length);
            });
        });

        describe('BULK-LOAD', function () {
            var tree;
            beforeEach(function () {
                tree = new PrTree();
                tree.insert(data);
            });

            it('Должен корректно вставить данные', function () {
                tree.insert(data);

                var treeItems = tree.getAll();
                for (var i = 0; i < data.length; i++) {
                    if (utilArray.indexOf(treeItems, data[i]) == -1) {
                        throw new Error('Элемент не вставился в дерево.');
                    }
                }
            });

            it('Должен произвести поиск и вернуть 6 элементов', function () {
                expect(tree.search([[0, 0], [25, 25]]).length).to.be(6);
            });

            it('Должен корректно удалить половину добавленных элементов', function () {
                var dataCount = data.length,
                    removeDataCount = Math.floor(dataCount / 2),
                    i;

                for (i = 0; i < removeDataCount; i++) {
                    tree.remove(data[i]);
                }

                var allItems = tree.getAll();
                for (i = 0; i < removeDataCount; i++) {
                    if (utilArray.indexOf(allItems, data[i]) != -1) {
                        throw new Error('Элемент не удалился из дерева');
                    }
                }

                expect(allItems.length).to.be(dataCount - removeDataCount);
            });

            it('Должен вернуть все листья дерева', function () {
                expect(tree.getAll().length).to.be(data.length);
            });
        });
    });

    provide();
});
