ymaps.modules.define(util.testfile(), [
    'util.dom.element',
    'util.dom.style',
    'util.dom.className',
    'util.math.areEqual',
    'util.css'
], function (provide, domElement, domStyle, className, areEqual, utilCss) {
    describe('util.dom', function () {
        before(function () {
            var elem = document.createElement('div');
            elem.innerHTML = '<div id="offset1">' +
                    '<div id="offset2" style="position: absolute; top: 100px; left: 100px;">' +
                        '<div id="offset3" style="margin-top: 10px; margin-left: 10px;">' +
                            '<span id="content" style="background-color: red;">' +
                                '1111' +
                            '</span>' +
                            '<span id="offset4" style="background-color:green;">' +
                                '<span id="offset5" style="width: 100px; height: 100px; background-color: red;">' +
                                    '2222' +
                                '</span>' +
                            '</span>' +
                        '</div>' +
                        '<div style="position: absolute; top:0; left:0;">' +
                            '<div style="height: 2500px;">12312</div>' +
                            '<div id="offset6" style=" margin-left: -100px; background-color: #4169e1; width: 150px; height: 150px;"></div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +

                '<div id="test"></div>' +
                '<div id="test2">' +
                    '<div class="c a" id="target1">' +
                        '<div class="a-b" id="target2"></div>' +
                    '</div>' +
                    '<div class="c">' +
                        '<div class="a"></div>' +
                    '</div>' +
                '</div>' +
                '<div id="create-test"></div>' +
                '<div style="font-family: Courier, monospace; font-size: 10px">' +
                    '<div style="width: 50em; height: 30em;">' +
                        '<div id="weird"' +
                            'style="position:relative;left:5px;top:6px;width: 50%; height:50%; border: 1em solid green; padding: 1em;"></div>' +
                    '</div>' +
                '</div>'
            document.body.insertBefore(elem, document.body.firstChild);
        });

        it('Должен правильно создать дом-ноду', function () {
            var node = domElement.create({
                tagName: 'test',
                className: 'create-test',
                attr: {
                    enabled: 'enabled'
                },
                css: {
                    position: 'absolute'
                },
                size: [10, 15],
                position: [20, 25],
                html: 'Hurray!',
                parentNode: document.getElementById('create-test')
            });

            expect(node.tagName.toLowerCase()).to.be('test');
            expect(node.className).to.be('create-test');
            expect(node.getAttribute('enabled')).to.be('enabled');
            expect(node.style.position).to.be('absolute');
            expect(node.style.width).to.be('10px');
            expect(node.style.height).to.be('15px');
            expect(node.style.left).to.be('20px');
            expect(node.style.top).to.be('25px');
            expect(node.innerHTML).to.be('Hurray!');
            expect(node.parentNode).to.be(document.getElementById('create-test'));
        });

        it('Должен правильно удалить ноду', function () {
            var node = domElement.create({
                parentNode: document.body
            });
            domElement.html(node, 'test<ymaps id="deleted">custom</ymaps>');
            domElement.remove(node);
            expect(document.getElementById('deleted')).not.to.be.ok();
        });

        it('Должен выставить innerHTML', function () {
            var text = '<ymaps><ymaps>Text</ymaps></ymaps>';
            var node = document.createElement(utilCss.addPrefix('wrap'));
            var parent = document.getElementById('test');
            var failed;

            try {
                domElement.html(node, text);
            }
            catch (e) {
                failed = true;
            }

            expect(failed).to.be(true);

            parent.appendChild(node);
            domElement.html(node, text);

            expect(node.innerHTML).to.be(text);
            expect(domElement.html(node)).to.be(text);
        });

        it('Должен корректно искать элементы по классам', function () {
            var context = document.getElementById('test2');
            expect(domElement.find(context, '.a')).to.be(document.getElementById('target1'));
            expect(domElement.find(context, '.a-b')).to.be(document.getElementById('target2'));
            expect(domElement.find(context, '.b')).not.to.be.ok();
        });

        it('Должен правильно рассчитывать размеры div', function () {
            var weirdDiv = document.getElementById('weird');
            var size = domStyle.getSize(weirdDiv);
            // Замечательный наш MSIE QM имеет свою боксмодель, там размеры другие
            var correctSize = navigator.userAgent.indexOf('MSIE') != -1 && document.compatMode == 'BackCompat' ?
                    [210, 110] :
                    [250, 150];

            expect(areEqual(size, correctSize)).to.be(true);

            domStyle.setSize(weirdDiv, size);
            var newSize = domStyle.getSize(weirdDiv);
            expect(areEqual(size, newSize)).to.be(true);
        });

        it('Должен корректно выставить position', function () {
            var weirdDiv = document.getElementById('weird');
            domStyle.css(weirdDiv, { position: 'absolute'});
            var position = domStyle.getOffset(weirdDiv);
            expect(areEqual(position, [5, 6])).to.be(true);

            domStyle.setPosition(weirdDiv, position);
            var newSize = domStyle.getOffset(weirdDiv);
            expect(areEqual(position, newSize)).to.be(true);
        });

        it('Должен корректно работать с offset', function () {
            var testElement = document.getElementById('test');
            testElement.scrollIntoView();

            domStyle.css(document.getElementById('weird'), { position: 'static'});
            var isFF = navigator.userAgent.indexOf("Firefox") != -1;
            var offset1Element = document.getElementById('offset1');
            var offset1 = domStyle.getOffset(offset1Element, true);
            var offset2 = domStyle.getOffset(document.getElementById('offset2'), true);
            var contentElement = document.getElementById('content');
            var offset3 = domStyle.getOffset(document.getElementById('offset3'), true);
            var offset4 = domStyle.getOffset(document.getElementById('offset4'), true);
            var offset5 = domStyle.getOffset(document.getElementById('offset5'), true);
            var offset6Element = document.getElementById('offset6');

            var offset6 = domStyle.getOffset(offset6Element, true);


            var validOffset = [0, 0];
            // Смещение из-за элемента weird
            expect(areEqual(offset1, validOffset)).to.be(true);

            validOffset[0] = validOffset[1] = 100;
            // Абсолютное позиционирование
            expect(areEqual(offset2, validOffset)).to.be(true);

           validOffset[0] = validOffset[1] = 110;
            // margin
            expect(areEqual(offset3, validOffset)).to.be(true);

            var contentElementSize = domStyle.getSize(contentElement);
            validOffset[0] += contentElementSize[0];

            // В FF добавляет пол пикселя, которые при округлении становятся пикселем.
            if (isFF) {
                validOffset[1] = 111;
            }

            // display: inline-block + padding от offset3
            expect(areEqual(offset4, validOffset)).to.be(true);
            expect(areEqual(offset5, validOffset)).to.be(true);

            validOffset[0] = 0;
            validOffset[1] = 2600;
            // смещение видимой области
            expect(areEqual(offset6, [0, 2600])).to.be(true);
        });

        it('Должен корректно добавлять и удалять классы в ноду', function () {
            var weirdDiv = document.getElementById('weird');
            expect(weirdDiv.className).to.be('');

            className.add(weirdDiv, 'test');
            expect(weirdDiv.className).to.be('test');
            expect(className.has(weirdDiv, 'test')).to.be(true);

            className.add(weirdDiv, 'test');
            expect(weirdDiv.className).to.be('test');

            className.add(weirdDiv, 'test2');
            className.add(weirdDiv, 'test3');
            className.remove(weirdDiv, 'test2');
            expect(weirdDiv.className).to.be('test test3');
            expect(className.has(weirdDiv, 'test3')).to.be(true);

            className.remove(weirdDiv, 'test3');
            expect(weirdDiv.className).to.be('test');

            className.remove(weirdDiv, 'test');
            expect(weirdDiv.className).to.be('');


            className.remove(weirdDiv, 'test');
            className.remove(weirdDiv, 'test3');
            expect(weirdDiv.className).to.be('');
        })
    });

    provide({});
});
