ymaps.modules.define(util.testfile(), [
    'control.Manager',

    'option.Manager',
    'data.Manager',
    'event.Manager',
    'templateLayoutFactory',
    'Monitor',
    'util.array',
    'util.dom.element',
    'util.dom.style',

    'Map',
    'expect'
], function (provide, ControlManager, OptionManager, DataManager, EventManager, templateLayoutFactory, Monitor,
             utilArray, utilDomElement, utilDomStyle, Map) {

    describe('control.Manager', function () {
        var myMap,
            control1,
            control2,
            control3,
            sizes = {
                'small': 'width: 50px; height: 20px;',
                'medium': 'width: 100px; height: 20px;',
                'large': 'width: 150px; height: 20px;'
            };

        var html = '<div style="{% if state.size == "small" %}{{ data.small }}{% endif %}{% if state.size == "medium" %}{{ data.medium }}{% endif %}{% if state.size ==" large" %}{{ data.large }}{% endif %}">{{options.text|"text"}}</div>';

        // Реализация интерфейса IControl
        var Control = function (parameters) {
            this.options = new OptionManager({
                maxWidth: [50, 100, 150],
                states: ['small', 'medium', 'large']
            });
            this.state = new DataManager({
                size: myMap.controls.state.get("size")
            });
            this.data = new DataManager(sizes);
            this.events = new EventManager();
            this.layout = new (templateLayoutFactory.createClass(html))({
                control: this,
                options: this.options,
                state: this.state,
                map: myMap
            });

            if (parameters && parameters.options) {
                this.options.set(parameters.options);
            }
        };

        Control.prototype = {
            setParent: function (parent) {
                if (parent) {
                    this.parent = parent;
                    parent.getChildElement(this).then(function (parentElement) {
                        if (this.parent) {
                            this.layout.setParentElement(parentElement);
                        }
                    }, this);
                    this._stateMonitor = new Monitor(parent.state);
                    this._stateMonitor.add('size', this._onStateChange, this);
                } else {
                    this.layout.setParentElement(null);
                    this._stateMonitor.destroy();
                }
            },

            getParent: function () {
                return this.parent;
            },

            _onStateChange: function (newState) {
                this.state.set('size', this.parent.state.get('size'));
            },

            getLayoutSync: function () {
                return this.layout;
            }
        };

        before(function () {
            myMap = new Map('map', {
                center: [55.777153093859496, 37.639130078124964],
                zoom: 10,
                controls: [],
                type: null
            });
        });

        after(function () {
            myMap.destroy();
        });

        beforeEach(function () {
            control1 = new Control();
            control2 = new Control();
            control3 = new Control();

            myMap.controls
                .add(control1)
                .add(control2)
                .add(control3);
        });

        afterEach(function () {
            myMap.controls
                .remove(control1)
                .remove(control2)
                .remove(control3);
        });

        it('Должен изменить состояние при добавлении ещё одного контрола', function () {
            var savedState = myMap.controls.state.get('size'),
                tempControl = new Control();

            myMap.controls.add(tempControl, {maxWidth: 900});
            expect(myMap.controls.state.get('size')).not.to.be(savedState);
            myMap.controls.remove(tempControl);
        });

        it('Должен изменить состояние при удалении одного контрола', function () {
            var tempControl = new Control(),
                savedState;

            myMap.controls.add(tempControl, {maxWidth: 900});
            savedState = myMap.controls.state.get('size');
            myMap.controls.remove(tempControl);

            expect(myMap.controls.state.get('size')).not.to.be(savedState);
        });

        it('Должен изменить состояние при изменении размера контейнера карты', function () {
            var controlManager = myMap.controls;

            var stateIndex = utilArray.indexOf(controlManager.options.get("states"), controlManager.state.get("size")),
                savedState,
                controlsMaxWidth = 0,
                userContainerElement = document.getElementById('map'),
                originalSize = utilDomStyle.getSize(userContainerElement);

            controlManager.each(function (item) {
                controlsMaxWidth += parseInt(item.options.get("maxWidth")[stateIndex]);
            });

            // меняем размер контейнера, так чтобы не все контролы помещались
            savedState = controlManager.state.get('size');
            utilDomStyle.setSize(userContainerElement, [controlsMaxWidth - 1, 300]);
            myMap.container.fitToViewport();

            expect(controlManager.state.get('size')).not.to.be(savedState);

            // возвращаем прежние размеры
            utilDomStyle.setSize(userContainerElement, originalSize);
        });

        it('Должен изменить контейнер контролу при измении floatIndex', function (done) {
            var controlManager = myMap.controls;

            var getElementIndex = function (parent, node) {
                var index = 0,
                    childs = parent.childNodes,
                    flag = true;

                while (index < childs.length && flag) {
                    if (childs[index] == node) {
                        flag = false;
                    }
                    index++;
                }
                return index;
            };

            var control = new Control({
                    options: {
                        floatIndex: 999
                    }
                }),
                rightControlsCount = 0,
                leftControlsCount = 0;

            controlManager.each(function (item) {
                var side = item.options.get('float', 'right');
                if (side == 'right') {
                    rightControlsCount++;
                } else if (side == 'left') {
                    leftControlsCount++;
                }
            });

            if (rightControlsCount > 1) {
                control.options.set('float', 'right');
            } else if (leftControlsCount > 1) {
                control.options.set('float', 'left');
            }
            controlManager.add(control);

            window.setTimeout(function () {
                var controlElement = control.getLayoutSync().getParentElement(),
                    parentElement = controlElement.parentNode;

                var indexBefore = getElementIndex(parentElement, controlElement),
                    indexAfter;
                control.options.set('floatIndex', -1);
                indexAfter = getElementIndex(parentElement, controlElement);

                expect(indexBefore).not.to.be(indexAfter);
                controlManager.remove(control);
                done();
            }, 50);
        });
    });

    provide();
});
