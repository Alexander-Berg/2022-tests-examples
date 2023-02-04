function mapTests (Y) {return {
    test: function () {
        myMap.events.add("click", function () {
            window.console && console.log("map click");
        });

        var overlayDataManager = new ymaps.data.Manager({
            content: "Вау!"
        });

        iconOverlay = new ymaps.overlay.html.Placemark(
            new ymaps.geometry.pixel.Point([920, 530]),
            {
                data: overlayDataManager
            }, {
                layout: "default#imageWithContent",
                contentLayout: ymaps.templateLayoutFactory.createClass('$[data.content]'),
                imageHref: "http://img1-fotki.yandex.net/get/6101/66910958.1c/0_STATIC586e9_c408dfd9_teaser",
                imageOffset: [-53, -35],
                imageSize: [106, 71],
                contentOffset: [60, 10]
            }
        );

        iconOverlay.options.setParent(myMap.options);
        iconOverlay.setMap(myMap);

        geoObject = new ymaps.Placemark({
            type: "Point",
            coordinates: [0, 55]
        }, {
            iconContent: "Wait...",
            hintContent: "Test",
            balloonContent: "Content",
            balloonContentHeader: "Header",
            balloonContentBody: "Body",
            balloonContentFooter: "Footer"
        }, {
            interactivityModel: "default#transparent",
//            iconCursor: "arrow",
            pane: "controls",
            iconContentLayout: ymaps.templateLayoutFactory.createClass('$[properties.iconContent] - $[geometry.coordinates]'),
            preset: "twirl#yellowStretchyIcon",
            hintHideTimeout: 0,
            hideIconOnBalloonOpen: false,
            balloonCloseButton: true//,
//            balloonContentLayout: ymaps.templateLayoutFactory.createClass('$[properties.balloonContentHeader]', {
//                build: function () {
//                    this.constructor.superclass.build.call(this);
//                    console.log(this.getData().options.get("preset"));
//                }
//            })
        });

        myMap.events.add("boundschange", function () {
//            geoObject.geometry.setCoordinates(myMap.getCenter());
        });

        geoObject.events.add("click", function () {
            window.console && console.log("geoObject click");
        });

        collection.add(geoObject);

        balloon = myMap.balloon.open([30, 50], {
            content: "Content",
            title: undefined
        }, {
            contentBodyLayout: ymaps.templateLayoutFactory.createClass('$[title|Без названия]')
        });

        var hint = myMap.hint.show([30, 40], {
            content: "Hint"
        });

        setTimeout(function () {
            geoObject.properties.set("iconContent", "Ok");
            geoObject.geometry.setCoordinates([0, 45]);
            geoObject.options.set("balloonOffset", [10, -40]);
//            geoObject.options.set("preset", "twirl#skiingIcon");

            geoObject.events.add("click", function (event) {
                window.console && console.log("click", event.get("globalPixelPosition"));
            });

            hint.setData({
                content: "Hint OK"
            });

            balloon.setData({
                contentHeader: "Header",
                contentBody: "Body <br/>Body <br/>Body <br/>Body <br/>Body <br/>Body",
                content: "Content <br/>Content <br/>Content <br/>Content <br/>Content <br/>Content <br/>Content",
                contentFooter: 0
            });

            overlayDataManager.set("content", "Wow!");
        }, 1000);


        geoObject1 = new ymaps.Placemark({
            type: "Point",
            coordinates: [0, 55]
        }, {
            closeButtonUrl: "http://vau.in.ua/i/close_button.gif",
            balloonContentHeader: "Header",
            balloonContentBody: "Body",
            balloonContentFooter: "Footer"
        }, {
            balloonCloseButtonLayout: ymaps.templateLayoutFactory.createClass("<img style='position: absolute; top: 4px; right: 4px; z-index: 10;' src='$[properties.closeButtonUrl]' />", {
                build: function () {
                    this.constructor.superclass.build.call(this);
                    var _this = this;
                    $(this.getParentElement()).click(function (e) {
                        _this._onClick(e);
                    });
                },

                _onClick: function (e) {
                    this.events.fire('close');
                }
            })
        });

        collection.add(geoObject1);
    },

    testPlacemarks: function () {
        myMap.geoObjects.events.add("add", function () {
            window.console && console.log("add");
        });

        geoObject1 = new ymaps.Placemark({
            type: "Point",
            coordinates: [10, 55]
        }, {
            hintContent: "Test",
            balloonContentHeader: "Header",
            balloonContentBody: "Body",
            balloonContentFooter: "Footer"
        }, {
            hideIconOnBalloonOpen: false,
            balloonCloseButton: true
        });

        geoObject1.events.add("mouseenter", function () {
            geoObject1.options.set("preset", "twirl#redDotIcon");
        });
        geoObject1.events.add("dragstart", function () {
            window.console && console.log("dragstart");
        });
        geoObject1.events.add("mouseleave", function () {
            geoObject1.options.unset("preset");
        });

        collection.add(geoObject1);

        geoObject2 = new ymaps.Placemark({
            type: "Point",
            coordinates: [10, 50]
        }, {
            iconContent: "0",
            balloonContentFooter: "Footer",
            hintContent: "blablabla"
        }, {
            preset: "twirl#greenIcon",
            pixelRendering: "static"
        });

        geoObject2.events.add("mouseenter", function () {
            geoObject2.options.set("preset", "twirl#redDotIcon");
        }).add("mouseleave", function () {
            geoObject2.options.set("preset", "twirl#greenIcon");
        });

        setInterval(function () {
            if (geoObject2.properties.get("iconContent") == "0") {
                geoObject2.properties.set("iconContent", "00");
            } else {
                geoObject2.properties.set("iconContent", "0");
            }
        }, 1000);

        collection.add(geoObject2);

        geoObject3 = new ymaps.Placemark({
           type: "Point",
           coordinates: [10, 59.95]
        }, {
            iconContent: 'Осло',
            hintContent: 'Петропавловская крепость',
            balloonContent: 'Hello<br/>My<br/>Little<br/>Baby' +
                    '<br /><img src="http://img-fotki.yandex.ru/get/3802/ymaps.3bb/0_3cf46_76bf9f13_M?rand=' + Math.random() +'" />' +
                    '<br /><img src="http://img-fotki.yandex.ru/get/3101/valokuva.e/0_1f73a_585f36cc_L?rand=' + Math.random() +'" />',
            balloonContentFooter: 'Goodbye'
        }, {
            preset: "twirl#darkorangeStretchyIcon",
            balloonDataSource: function (geoObject, openBalloon) {
                setTimeout(function () {
                    //openBalloon(geoObject.properties);
                }, 1000);
            },
            hintOffset: [1, 1]
        });

//        geoObject3.events
//            .add('mouseenter', function() {
//                window.console && console.log('mouseenter');
//                geoObject3.options.set('contentIconNoPadding', true);
//                geoObject3.properties.set('iconContent', '<img src="http://www.spb-tours.ru/sightseeing/images/2_petropavlovka1-sm.jpg" />');
//            })
//            .add('mouseleave', function() {
//                window.console && console.log('mouseleave');
//                geoObject3.options.unset('contentIconNoPadding');
//                geoObject3.properties.set('iconContent', 'Санкт-Петербург');
//            });
        geoObject3.events.add("click", function () {
            window.console && console.log("click");
        });

        collection.add(geoObject3);

        geoObject4 = new ymaps.Placemark({
           type: "Point",
           coordinates: [10, 45]
        }, {}, {
            preset: "twirl#violetStretchyIcon",
            openBalloonOnClick: false,
            showHintOnHover: false
        });

        geoObject4.events
            .add('mouseenter', function(e) {
                if (!geoObject4.state.get('drag')) {
                    geoObject4.properties.set('iconContent', 'Киров');
                }
            })
            .add('mouseleave', function() {
                if (!geoObject4.state.get('drag')) {
                    geoObject4.properties.unset('iconContent');
                }
            })
            .add('dragstart', function(e) {
                geoObject4.properties.set('iconContent', '<img src="http://ajaxy.ru/yandex/petr.png" />');
                geoObject4.options.set('iconContentPadding', false);
            })
            .add('dragend', function() {
                geoObject4.options.unset('iconContentPadding');
                geoObject4.properties.set('iconContent', geoObject4.state.get('hover') ? 'Киров' : '');
            });

        collection.add(geoObject4);
    },

    testLine: function () {
        line1 = new ymaps.GeoObject({
            geometry: {
                type: "LineString",
                coordinates: [[20, 40], [20, 50], [25, 55], [20, 60]]
            },
            properties: {
                balloonContentBody: '<br /><img src="http://img-fotki.yandex.ru/get/4214/alppo.1/0_3c66f_b1e3ab64_L?rand=' + Math.random() +'" />',
                balloonContentFooter: "Description"
            }
        }, {
            geodesic: true,
            draggable: true,
            strokeColor: '#1a3dc188',
            strokeWidth: 20
        });

        myMap.geoObjects.add(line1);
    },

    testPolygon: function () {
        var geometry = {
                type: "Polygon",
                coordinates: [
                    [[60, 48], [62, 60], [80, 62], [80, 48], [60, 48]],
                    [[69, 45], [68, 55], [86, 56], [87, 43], [69, 45]],
                    [[72, 59], [72, 65], [86, 64], [87, 59], [72, 59]]
                ]
            },
            properties = new ymaps.data.Manager({
                balloonContentBody: "Name",
                balloonContentFooter: '<br /><img src="http://img-fotki.yandex.ru/get/4411/15828326.45/0_6094a_5b872809_L?rand=' + Math.random() +'" />' +
                '<br /><img src="http://img-fotki.yandex.ru/get/5202/novladimi.e/0_35e3b_83fc0205_L?rand=' + Math.random() +'" />',
                hintContent: "Polygon"
            }),
            geoObject = new ymaps.GeoObject({
                geometry: geometry,
                properties: properties
            }, {
                balloonAutoPan: false,
                geodesic: true,
                cursor: "crosshair",
                draggable: true,
                strokeColor: '#ff0000',
                fillColor: '#6699cc',
                strokeWidth: 10,
                fillImageHref: 'http://yandex.st/lego/_/mjhyREEiYd6IeT2Qw4sUlHcNdto.png',
                fillMethod: 'tile'
//                overlayFactory: ymaps.geoObject.overlayFactory.staticGraphics
            });

        collection.add(geoObject);

        var marker = new ymaps.Placemark({
            type: "Point",
            coordinates: [60, 48]
        }, {}, {
            draggable: true
        });

        marker.events.add("drag", function (e) {
            var res = geoObject.geometry.getClosest(e.get("target").geometry.getCoordinates());
            window.console && console.log(
                "distance: " + res.distance,
                "position: " + res.position,
                "closestPointIndex: " + res.closestPointIndex,
                "pathIndex: " + res.pathIndex
            );
        });

        myMap.geoObjects.add(marker);

        var geometryPolygon1 = {
                type: 'Polygon',
                coordinates: [
                    [[35, 55], [40, 59], [45, 55], [32, 57.5], [48, 57.5]],
                    []
                ],
                fillRule: "nonZero"
            };
        var propertiesPolygon1 = new ymaps.data.Manager({
            hintContent: "DiskoSuperStar"
        });

        var polygon = new ymaps.GeoObject({
            geometry: geometryPolygon1,
            properties: propertiesPolygon1
        }, {
            draggable: true,
            fillColor: "rgba(0, 0, 200, 0.5)",
            strokeColor: "rgba(0, 200, 0, 0.5)",
            strokeWidth: 5,
            fillImageHref: 'http://yandex.st/lego/_/mjhyREEiYd6IeT2Qw4sUlHcNdto.png',
            fillMethod: 'tile',
            openEmptyBalloon: false
        });

        collection.add(polygon);

        window.polygon = geoObject;
    },

    testCircle: function () {
        var circle = new ymaps.GeoObject({
            geometry: {
                type: "Circle",
//                coordinates: [35, 50],
                coordinates: [30, 50],
                radius: 100000
            }
        }, {
            geodesic: true,
            draggable: true,
            strokeWidth: 5,
            strokeColor: "rgba(200, 200, 0, 0.5)",
            fillColor: "rgba(0, 0, 200, 0.5)",
            overlayFactory: ymaps.geoObject.overlayFactory.staticGraphics
        });

        collection.add(circle);

        window.circle = circle;
    },

    testRectangle: function () {
        var rectangle = new ymaps.GeoObject({
            geometry: {
                type: "Rectangle",
                coordinates: [[30, 40], [40, 45]]
            },
            properties: {
                balloonContentBody: "Name"
            }
        }, {
            geodesic: true,
            draggable: true,
            fillColor: "rgba(100, 10, 0, 0.5)",
//            overlayFactory: ymaps.geoObject.overlayFactory.staticGraphics,
            coordRendering: 'straightPath',
            borderRadius:10
        });

        collection.add(rectangle);

        window.rectangle = rectangle;
    },

    testPolyToLine: function () {
        var geoObject = new ymaps.GeoObject({
                geometry: {
                    type: "Polygon",
                    coordinates: [
                        [[30, 50], [50, 70]],
                        [[30, 70], [50, 50]],
                        [[30, 60], [50, 60]]
                    ]
                }
            }, {
                geodesic: true,
                draggable: true,
                strokeColor: '#ff0000',
                fillColor: '#6699cc',
                strokeWidth: 10,
                opacity: "0.5"
            });

        collection.add(geoObject);
    },

    _testArray: function () {
        var myGroup = new ymaps.GeoObjectArray({}, {
            preset: "twirl#airplaneIcon",
            strokeWidth: 4,
            geodesic: true
        });

        myGroup.add(new ymaps.Placemark([13.38, 52.51]));
        myGroup.add(new ymaps.Placemark([30.30, 50.27]));
        myGroup.add(new ymaps.Polyline([[13.38, 52.51], [30.30, 50.27]]));

        myMap.geoObjects.add(myGroup);
        myMap.setBounds(myGroup.getBounds());
    },

    _testCollection: function () {
        var myCollection = new ymaps.GeoObjectCollection();

        myCollection.add(new ymaps.Placemark([37.61, 55.75]));
        myCollection.add(new ymaps.Placemark([13.38, 52.51]));
        myCollection.add(new ymaps.Placemark([30.30, 50.27]));

        myCollection.events
            .add("mouseenter", function () {
                myCollection.options.set("preset", "twirl#redIcon");
            })
            .add("mouseleave", function () {
                myCollection.options.unset("preset");
            });

        myMap.geoObjects.add(myCollection);
        myMap.setBounds(myCollection.getBounds());
    },

    _testB: function () {
        myMap.events.add('click', function (e) {
            var coords = e.get('coordPosition');
            // Отправим запрос на геокодирование
            ymaps.geocode(coords).then(function (res) {
                var names = [];
                // Переберём все найденные результаты и
                // запишем имена найденный объектов в массив names
                res.geoObjects.each(function (obj) {
                    names.push(obj.properties.get('name'));
                    if (!myMap.balloon.isOpen()) {
                        myMap.balloon.open(coords, {
                            contentHeader: 'Событие!',
                            contentBody: names[0] +
                                '<p>Координаты точки: ' + [
                                coords[0].toPrecision(6),
                                coords[1].toPrecision(6)
                            ].join(', ') + '</p>',
                            contentFooter: '<sup>Адрес обьекта</sup>'
                        });
                    } else {
                        myMap.balloon.close();
                    }
                });
            });
        });

        // Создаем геодезический круг радиусом 1000 километров.
        var circle = new ymaps.Circle([[50, 50], 1000000], {}, {
            draggable: true
        });
        // Добавляем круг на карту.
        myMap.geoObjects.add(circle);

        // Создаем прямоугольник на основе границы круга.
        var rectangle = new ymaps.Rectangle(circle.geometry.getBounds(), {}, {
            fill: false,
            coordRendering: "boundsPath",
            strokeWidth: 4
        });
        // Добавляем прямоугольник на карту на карту.
        myMap.geoObjects.add(rectangle);

        // При изменении геометрии круга обновляем координаты прямоугольника.
        circle.geometry.events.add("change", function (event) {
            this.geometry.setCoordinates(event.get("target").getBounds());
        }, rectangle);
    },

    testImageContent: function () {
        imageContent = new ymaps.Placemark([-15, 45], {
            iconContent: '<img src="//ajaxy.ru/yandex/petr.png" />'
        }, {
            preset: 'twirl#nightStretchyIcon',
            iconContentPadding: false
        });

        myMap.geoObjects.add(imageContent);
    },

    testDD: function () {
        var geom = new ymaps.geometry.Polygon([[[1, 2], [3, 4], [5, 6]]]);
        geom.setMap(myMap);
        geom.options.setParent(myMap.options);

        console.log(geom.getBounds());
    }
};}
