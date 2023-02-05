ymaps.modules.define(util.testfile(), [
    'util.dom.element',
    'util.css',
    'expect'
], function (provide, domElement, utilCss) {
    describe('util.dom.element', function () {
        var elements = [],
            elemsContainer = domElement.create(),
            elementClass = 'class',
            elementId = 'id';

        before(function () {
            document.body.appendChild(elemsContainer);

            for (var i = 0; i < 10; i++) {
                elements.push(
                    domElement.create({
                        tagName: 'div',
                        className: elementClass,
                        parentNode: elemsContainer
                    }),
                    domElement.create({
                        tagName: 'div',
                        attr: {
                            id: elementId
                        },
                        parentNode: elemsContainer
                    }),
                    domElement.create({
                        tagName: 'ymaps',
                        className: utilCss.addPrefix('class'),
                        parentNode: elemsContainer
                    })
                );
            }
        });

        after(function () {
            elemsContainer.parentNode.removeChild(elemsContainer);
        });

        describe('find', function () {
            it('Должен найти 10 элементов по ID', function () {
                var foundElements = domElement.find(elemsContainer, '#id', true);
                expect(foundElements).to.have.length(10);
            });

            it('Должен вернуть только 1 элемент по ID из 10', function () {
                var foundElement = domElement.find(elemsContainer, '#id', false);
                expect(domElement.isNode(foundElement)).to.be.ok();
            });

            it('Должен найти 10 элементов по className', function () {
                var foundElements = domElement.find(elemsContainer, '.class', true);
                expect(foundElements).to.have.length(10);
            });

            it('Должен вернуть только 1 элемент по className из 10', function () {
                var foundElement = domElement.find(elemsContainer, '.class', false);
                expect(domElement.isNode(foundElement)).to.be.ok();
            });

            it('Должен найти 10 элементов по tagName#ID', function () {
                var foundElements = domElement.find(elemsContainer, 'div#id', true);
                expect(foundElements).to.have.length(10);
            });

            it('Должен вернуть только 1 элемент по tagName#ID из 10', function () {
                var foundElement = domElement.find(elemsContainer, 'div#id', false);
                expect(domElement.isNode(foundElement)).to.be.ok();
            });

            it('Должен найти 10 элементов по tagName.className', function () {
                var foundElements = domElement.find(elemsContainer, 'div.class', true);
                expect(foundElements).to.have.length(10);
            });

            it('Должен вернуть только 1 элемент по tagName.className из 10', function () {
                var foundElement = domElement.find(elemsContainer, 'div.class', false);
                expect(domElement.isNode(foundElement)).to.be.ok();
            });
        });

        describe('findByPrefixedClass', function () {
            it('Должен найти 10 элементов по .prefix-className', function () {
                var foundElements = domElement.findByPrefixedClass(elemsContainer, 'class', true);
                expect(foundElements).to.have.length(10);
            });

            it('Должен вернуть только 1 элемент по .prefix-className из 10', function () {
                var foundElement = domElement.findByPrefixedClass(elemsContainer, 'class', false);
                expect(domElement.isNode(foundElement)).to.be.ok();
            });
        });
    });
    provide();
});
