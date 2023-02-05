ymaps.modules.define(util.testfile(), [
    'util.nodeSize',
    'util.dom.style'
], function (provide, nodeSize, domStyle) {
    var container;
    createStyles();
    createBlocks();

    describe('util.nodeSize', function () {
        afterEach(function () {
            clearContainer(container);
        });

        it('Должен правильно синхронно измерять размеры элемента', function () {
            var h = 100;
            var w = 150;
            container.innerHTML = "<div style='width: " + w + "px; height: " + h + "px;'></div>";
            var data = nodeSize.computeByContent(container, {});
            expect(data.width).to.be(w);
            expect(data.height).to.be(h);
        });

        describe('Должен вернуть максимально разрешенный размер, если размеры элемента больше', function () {
            it('Должна вернуться масимальная ширина и высота', function () {
                var h = 150;
                var w = 200;
                var maxH = 120;
                var maxW = 120;
                container.innerHTML = "<div style='width: " + w + "px; height: " + h + "px;'></div>";
                var data = nodeSize.computeByContent(container, {
                    maxWidth: maxW,
                    maxHeight: maxH,
                    scrollX: true,
                    scrollY: true
                });
                expect(data.width).to.be(maxW);
                expect(data.height).to.be(maxH);
            });

            it('Должна вернуться масимальная ширина и оригинальная высота элемента', function () {
                var h = 100;
                var w = 200;
                var maxH = 120;
                var maxW = 120;
                container.innerHTML = "<div style='width: " + w + "px; height: " + h + "px;'></div>";
                var data = nodeSize.computeByContent(container, {
                    maxWidth: maxW,
                    maxHeight: maxH
                });

                expect(data.width).to.be(maxW);
                expect(data.height).to.be(h);
            });

            it('Должна вернуться масимальная высота и оригинальная ширина элемента', function () {
                var h = 200;
                var w = 100;
                var maxH = 120;
                var maxW = 120;
                container.innerHTML = "<div style='width: " + w + "px; height: " + h + "px;'></div>";
                var data = nodeSize.computeByContent(container, {
                    maxWidth: maxW,
                    maxHeight: maxH
                });
                expect(data.width).to.be(w);
                expect(data.height).to.be(maxH);
            });
        });

        it('Должен правильно рассчитать размеры с учетом padding и border', function () {
            var h = 101;
            var w = 101;
            var maxH = 200;
            var maxW = 200;
            domStyle.css(container, {
                padding: '3px 6px 9px 12px',
                borderWidth: '12px 9px 6px 3px',
                borderStyle: 'solid',
                borderColor: 'black'
            });

            container.innerHTML = "<div style='width: " + w + "px; height: " + h + "px;'></div>";
            var data = nodeSize.computeByContent(container, {
                maxWidth: maxW,
                maxHeight: maxH,
                scrollX: true,
                scrollY: true
            });
            expect(data.width).to.be(w);
            expect(data.height).to.be(h);
        });
    });

    function createBlocks() {
        container = document.createElement('div');
        container.setAttribute('id', 'container');
        document.body.appendChild(container);
    }

    function clearContainer(container) {
        container.innerHTML = '';
        domStyle.css(container, {
            width: '',
            height: '',
            // IE показывает скролбары, если не hidden
            overflowX: 'hidden',
            overflowY: 'hidden',
            padding: '',
            margin: '',
            border: ''
        });
    }

    function createStyles() {
        var styles = document.createElement('style');
        styles.innerHTML = '\
            #container { \
                position: absolute; \
                top: 1px; \
                left: 1px; \
            }';
        document.head.appendChild(styles);
    }

    provide();
});
