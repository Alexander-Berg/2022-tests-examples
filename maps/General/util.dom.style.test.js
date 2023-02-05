ymaps.modules.define(util.testfile(), [
    'util.dom.element',
    'util.dom.style'
], function (provide, domElement, domStyle) {
    describe('util.dom.style', function () {
        it('Должен добавить и удалить фильтр на ноду в дереве', function () {
            if (navigator.userAgent.indexOf('MSIE') == -1 || document.documentMode >= 9) {
                return true;
            }

            var node = domElement.create({
                    nodeName: 'div',
                    parentNode: document.body,
                    className: 'filtered',
                    css: {
                        filter: 'progid:DXImageTransform.Microsoft.Alpha(opacity=50)'
                    }
                });

            domStyle.scaledBackgroundImage(node, 'http://browser.yandex.ru/freeze/6gD9FUzNV1E0FwBCRUihoryYAd0.png');
            expect(node.filters.length).to.be(2);

            domStyle.scaledBackgroundImage(node, null);
            expect(node.filters.length).to.be(1);
        });

        it('Должен добавить и удалить фильтр на ноду вне дерева', function () {
            if (navigator.userAgent.indexOf('MSIE') == -1 || document.documentMode >= 9) {
                return true;
            }

            var node = domElement.create({
                    nodeName: 'div',
                    className: 'filtered',
                    css: {
                        filter: 'progid:DXImageTransform.Microsoft.Alpha(opacity=50)'
                    }
                });

            domStyle.scaledBackgroundImage(node, 'http://browser.yandex.ru/freeze/6gD9FUzNV1E0FwBCRUihoryYAd0.png');
            expect(node.style.filter.match(/progid/g).length).to.be(2);

            domStyle.scaledBackgroundImage(node, null);
            expect(node.style.filter.match(/progid/g).length).to.be(1);

            document.body.appendChild(node);
        });
    });

    provide({});
});
