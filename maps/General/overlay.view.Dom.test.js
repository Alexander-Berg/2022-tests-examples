ymaps.modules.define(util.testfile(), [
    "Map",
    "overlay.view.Dom",
    "overlay.html.Placemark",
    "geometry.pixel.Point",
    "overlay.html.Rectangle",
    "geometry.pixel.Rectangle",
    "geoObject.view.overlayMapping",
    "option.Manager",
    "option.Mapper",
    "templateLayoutFactory",
    "util.array",
    'layout.storage',
    'geometry.pixel.Polygon',
    'shape.Polygon',
    'vow',

    "layout.Image",
    "layout.RectangleLayout",
    "geoObject.metaOptions",
    "theme.islands.geoObject.meta.full"
], function (
    provide, Map, DomView, HtmlPlacemark, PixelPointGeometry, HtmlRectangle, PixelRectangleGeometry,
    overlayMapping, OptionManager, OptionMapper, templateLayoutFactory, array, layoutStorage,
    GeometryPixelPolygon, PolygonShape, vow
) {
    describe("overlay.view.Dom", function () {
        var shapeSpy = sinon.spy(),
            layoutSpy = sinon.spy(),
            emptinessSpy = sinon.spy(),
            spyContext = {},
            map,
            domView;

        before(function () {
            map = new Map('map', {
                center: [55.751574, 37.573856],
                controls: [],
                type: null,
                zoom: 3
            });
        });

        afterEach(function () {
            shapeSpy.reset();
            emptinessSpy.reset();
            layoutSpy.reset();

            if (domView) {
                domView.destroy();
                domView = null;
            }
        });

        after(function () {
            map.destroy();
        });

        //**************************** Утилиты ****************************

        function createDomView (params) {
            var donorOverlay = params.overlay;

            return new DomView({
                position: params.position,
                data: donorOverlay.getData(),
                options: donorOverlay.options,
                eventMapper: donorOverlay.getEventMapper(),
                pane: resolvePane(donorOverlay.options.get("pane", donorOverlay.getDefaultPane())),
                zIndex: donorOverlay.options.get("zIndex"),
                element: {
                    className: 'dom-view'
                },
                layout: {
                    defaultValue: params.layout
                },
                layoutParameters: {
                    disableDomEventListening: false
                }
            }, {
                onShapeChange: params.onShapeChange,
                onLayoutChange: params.onLayoutChange,
                onEmptinessChange: params.onEmptinessChange
            });
        }

        function createPointParams () {
            var geometry = new PixelPointGeometry([500, 500]),
                data = {
                    testData: "data"
                },
                overlay = new HtmlPlacemark(geometry, data, {});

            setupOverlayOptions(overlay);

            return {
                overlay: overlay,
                position: geometry.getCoordinates(),
                layout: "default#image",
                data: data,
                onShapeChange: {
                    callback: shapeSpy,
                    context: spyContext
                },
                onLayoutChange: {
                    callback: layoutSpy,
                    context: spyContext
                },
                onEmptinessChange: {
                    callback: emptinessSpy,
                    context: spyContext
                }
            };
        }

        function setupOverlayOptions (overlay) {
            var overlayOptionMapper = new OptionMapper();
            overlayMapping.setupMapping(
                overlayOptionMapper, overlay.options.getName(),
                overlay.getGeometry().getType()
            );

            var optionManager = new OptionManager(
                null, map.geoObjects.options, "geoObject", overlayOptionMapper
            );

            overlay.options.setParent(optionManager);
        }

        function resolvePane (paneKey) {
            return map.panes.get(paneKey);
        }

        function calcElementPosition (element) {
            return [parseFloat(element.style.left), parseFloat(element.style.top)];
        }

        function mockGetShape (coords) {
            return function () {
                return new PolygonShape(
                    new GeometryPixelPolygon(coords, 'evenOdd')
                );
            };
        }

        // Обеспечивает уникальный незагруженный макет для теста, т.к. layoutStorage
        // не позволяет использовать макет несколько раз.
        var cnt = 0;
        function defineAsyncLayout (id) {
            var key = "test.overlay.view.Dom#" + id + cnt++;
            layoutStorage.define(key, function (provide) {
                provide.async(vow.delay(
                    templateLayoutFactory.createClass('<div id="' + id + '">{{ testData }}</div>', {
                        getShape: mockGetShape([[[0, 0], [1, 1]]])
                    }),
                    10));
            });
            return key;
        }

        var testDataLayout = templateLayoutFactory.createClass(
            '<div id="testDataContainer">{{ testData }}</div>', {
                getShape: mockGetShape([[[0, 0], [1, 1]]])
            }
        );

        //**************************** Пейн ****************************

        it("После создания DOM элемент должен находится в пейне", function (done) {
            domView = createDomView(createPointParams());

            expect(domView.getElement().parentNode).to.be(domView.getPane().getElement());

            done();
        });

        it("При смене пейна DOM элемент должен переместится в новый пейн", function (done) {
            domView = createDomView(createPointParams());

            var overlapsPane = resolvePane("overlaps");

            domView.setPane(overlapsPane);

            expect(domView.getPane()).to.be(overlapsPane);
            expect(domView.getElement().parentNode).to.be(overlapsPane.getElement());

            done();
        });

        it("При уничтожении DOM элемент должен удалитья из пейна", function (done) {
            domView = createDomView(createPointParams());

            var pane = domView.getPane(),
                element = domView.getElement();

            domView.destroy();
            domView = null;

            expect(element.parentNode).to.not.be(pane.getElement());

            done();
        });

        //**************************** Позиционирование ****************************

        it("После создания DOM элемент в нужной позиции", function (done) {
            domView = createDomView(createPointParams());

            expect(
                calcElementPosition(domView.getElement())
            ).to.eql(
                array.map(
                    domView.getPane().toClientPixels([500, 500]),
                    Math.round
                )
            );

            done();
        });

        it("Смена позиции перемещает DOM элемент", function (done) {
            domView = createDomView(createPointParams());

            var newPosition = [1200, 600];
            domView.setPosition(newPosition);

            expect(
                calcElementPosition(domView.getElement())
            ).to.eql(
                array.map(
                    domView.getPane().toClientPixels(newPosition),
                    Math.round
                )
            );

            done();
        });

        //**************************** Макет ****************************

        it("Создание с синхронным макетом", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", testDataLayout);

            domView = createDomView(params);

            expect(layoutSpy.called).to.not.be.ok();
            expect(domView.getLayoutSync().getParentElement()).to.be(domView.getElement());
            expect(document.getElementById("testDataContainer")).to.be.ok();

            done();
        });

        it("Создание с асинхронным макетом", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", defineAsyncLayout("testAsyncLayout"));

            domView = createDomView(params);

            expect(layoutSpy.called).to.not.be.ok();
            expect(domView.getLayoutSync()).to.not.be.ok();

            domView.getLayout().done(function (layout) {
                expect(layoutSpy.callCount).to.be(1);
                expect(domView.getLayoutSync()).to.be(layout);
                expect(layout.getParentElement()).to.be(domView.getElement());
                expect(document.getElementById("testAsyncLayout")).to.be.ok();
                done();
            }, function (e) {
                expect().fail(e.message);
                done();
            });
        });

        it("Создание с несуществующим макетом", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", "test.overlay.view.Dom#fakeLayout");

            domView = createDomView(params);

            domView.getLayout().done(function () {
                expect().fail("Promise resolved");
                done();
            }, function (e) {
                expect(layoutSpy.called).to.not.be.ok();
                expect(domView.getLayoutSync()).to.not.be.ok();
                expect(e.message).to.be("Layout wasn't found");
                done();
            });
        });

        it("Смена синхронного макета на синхронный", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", testDataLayout);

            domView = createDomView(params);

            var oldLayout = domView.getLayoutSync();

            params.overlay.options.set("layout", templateLayoutFactory.createClass(
                '<div id="testNewLayoutContainer">text: {{ testData }}</div>'
            ));

            expect(layoutSpy.callCount).to.be(1);
            expect(oldLayout.getParentElement()).to.not.be.ok();
            expect(domView.getLayoutSync().getParentElement()).to.be(domView.getElement());
            expect(document.getElementById("testNewLayoutContainer")).to.be.ok();

            done();
        });

        it("Смена синхронного макета на асинхронный", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", testDataLayout);

            domView = createDomView(params);

            params.overlay.options.set("layout", defineAsyncLayout("testAsyncLayout"));

            domView.getLayout().done(function (layout) {
                expect(layoutSpy.callCount).to.be(1);
                expect(oldLayout.getParentElement()).to.not.be.ok();
                expect(domView.getLayoutSync()).to.be(layout);
                expect(document.getElementById("testAsyncLayout")).to.be.ok();
                expect(layout.getParentElement()).to.be(domView.getElement());
                done();
            }, function (e) {
                expect().fail(e.message);
                done();
            });

            var oldLayout = domView.getLayoutSync();
            expect(oldLayout.getParentElement()).to.be(domView.getElement());
        });

        it("Смена асинхронного макета на асинхронный", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", defineAsyncLayout("testAsyncLayout"));

            domView = createDomView(params);

            domView.getLayout().done(function (layout) {
                expect(layoutSpy.callCount).to.be(1);
                expect(domView.getLayoutSync()).to.be(layout);
                expect(document.getElementById("testAsyncLayout")).to.not.be.ok();
                expect(document.getElementById("otherAsyncLayout")).to.be.ok();
                expect(layout.getParentElement()).to.be(domView.getElement());
                done();
            }, function (e) {
                expect().fail(e.message);
                done();
            });

            params.overlay.options.set("layout", defineAsyncLayout("otherAsyncLayout"));
        });

        it("Смена асинхронного макета на синхронный", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", defineAsyncLayout("testAsyncLayout"));

            domView = createDomView(params);

            domView.getLayout().done(function (layout) {
                expect(layoutSpy.callCount).to.be(1);
                expect(layout).to.be(newLayout);
                expect(domView.getLayoutSync()).to.be(layout);
                expect(layout.getParentElement()).to.be(domView.getElement());
                done();
            }, function (e) {
                expect().fail(e.message);
                done();
            });

            params.overlay.options.set("layout", testDataLayout);

            var newLayout = domView.getLayoutSync();

            expect(layoutSpy.callCount).to.be(1);
            expect(newLayout.getParentElement()).to.be(domView.getElement());
            expect(document.getElementById("testDataContainer")).to.be.ok();
        });

        it("Смена на несуществующий макет, а затем на асинхронный", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", testDataLayout);

            domView = createDomView(params);

            params.overlay.options.set("layout", "test.overlay.view.Dom#fakeLayout");

            domView.getLayout().done(function () {
                expect().fail("Promise resolved");
                done();
            }, function (e) {
                expect(layoutSpy.callCount).to.be(1);
                expect(domView.getLayoutSync()).to.not.be.ok();
                expect(e.message).to.be("Layout wasn't found");

                params.overlay.options.set("layout", defineAsyncLayout("testAsyncLayout"));

                domView.getLayout().done(function (layout) {
                    expect(layoutSpy.callCount).to.be(2);
                    expect(domView.getLayoutSync()).to.be(layout);
                    expect(layout.getParentElement()).to.be(domView.getElement());
                    done();
                }, function (e) {
                    expect().fail(e.message);
                    done();
                });
            });
        });

        //**************************** Данные ****************************

        it("Смена данных должна отразится в макете", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", testDataLayout);

            domView = createDomView(params);
            domView.setData({
                testData: "newData"
            });

            var layout = domView.getLayoutSync(),
                layoutElement = document.getElementById("testDataContainer");

            expect(layoutSpy.called).to.not.be.ok();
            expect(layout.getParentElement()).to.be(domView.getElement());
            expect(layoutElement.innerHTML).to.be("newData");

            done();
        });

        //**************************** Шейп ****************************

        it("Шейп после создания при синхронном макете", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", testDataLayout);

            domView = createDomView(params);

            expect(shapeSpy.called).to.not.be.ok();
            expect(domView.getShape()).to.be.ok();
            expect(domView.getShape().getGeometry().getCoordinates()).to.eql([[[0, 0], [1, 1]]]);

            done();
        });

        it("Шейп после создания при асинхронном макете", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", defineAsyncLayout("testAsyncLayout"));

            domView = createDomView(params);

            expect(shapeSpy.called).to.not.be.ok();
            expect(domView.getShape()).to.not.be.ok();

            var callbacks = domView.getCallbacks();
            callbacks.onShapeChange = {
                callback: function () {
                    expect(domView.getShape()).to.be.ok();
                    expect(domView.getShape().getGeometry().getCoordinates()).to.eql([[[0, 0], [1, 1]]]);
                    done();
                }
            };
            domView.setCallbacks(callbacks);
        });

        it("Смена шейпа при смене макета", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", testDataLayout);
            params.onShapeChange = {
                callback: function () {
                    expect(domView.getShape()).to.be.ok();
                    expect(domView.getShape().getGeometry().getCoordinates()).to.eql([[[1, 1], [2, 2]]]);
                    done();
                },
                context: this
            };

            domView = createDomView(params);

            params.overlay.options.set("layout", templateLayoutFactory.createClass(
                '<div id="testDataContainer">{{ testData }}</div>', {
                    getShape: mockGetShape([[[1, 1], [2, 2]]])
                }
            ));
        });

        //**************************** Emptiness ****************************

        it("Emptiness после создания при синхронном макете", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", testDataLayout);

            domView = createDomView(params);

            expect(emptinessSpy.called).to.not.be.ok();
            expect(domView.isEmpty()).to.not.be.ok();

            done();
        });

        it("Emptiness после создания при асинхронном макете", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", defineAsyncLayout("testAsyncLayout"));

            domView = createDomView(params);

            expect(emptinessSpy.called).to.not.be.ok();
            expect(domView.isEmpty()).to.be.ok();

            var callbacks = domView.getCallbacks();
            callbacks.onEmptinessChange = {
                callback: function () {
                    expect(domView.isEmpty()).to.not.be.ok();
                    done();
                }
            };
            domView.setCallbacks(callbacks);
        });

        it("Смена emptiness при смене макета", function (done) {
            var params = createPointParams();
            params.overlay.options.set("layout", testDataLayout);
            params.onEmptinessChange = {
                callback: function () {
                    expect(domView.isEmpty()).to.be.ok();
                    done();
                },
                context: this
            };

            domView = createDomView(params);

            params.overlay.options.set("layout", templateLayoutFactory.createClass(''));
        });
    });

    provide();
});
