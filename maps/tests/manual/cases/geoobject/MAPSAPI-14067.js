/* global ymaps, GeoMotionObject */
ymaps.ready(function () {
    "use strict";

    var map = new ymaps.Map("map", {
        center: [55.7571, 37.61681], // ~msk
        zoom: 15.5,
        controls: []
    }, {
        avoidFractionalZoom: false
    });

    // ============= Create cars

    // Sprite Cars with iconPixelPerfect:false
    var car1 = new GeoMotionObject({
        geometry: {
            type: "Point",
            coordinates: [55.7571, 37.61681]
        }
    }, {
        iconLayout: ymaps.templateLayoutFactory.createClass('<div class="car1 car1_direction_$[properties.direction] car1_state_$[properties.state]"></div>'),
        iconImageSize: [54, 54],
        iconOffset: [-27, -27],
        interactivityModel: 'default#opaque',
        speedFactor: 2,
        countSides: 16
    });

    ymaps.route(
        [
            [55.7571, 37.61681],
            [55.7171, 37.68681]
        ]
    ).then(
        function (route) {
            var path = route.getPaths().get(0);

            car1.moveOnPath(path).then(function (status) {
                console.log('car 1', status);
            });

        }, function (error) {
            console.error("car 1: " + error.message);
        });

    var car2 = new GeoMotionObject({
        geometry: {
            type: "Point",
            coordinates: [55.7581, 37.61681]
        }
    }, {
        iconLayout: ymaps.templateLayoutFactory.createClass('<div class="car1 car1_direction_$[properties.direction] car1_state_$[properties.state]"></div>'),
        iconImageSize: [54, 54],
        iconOffset: [-27, -27],
        interactivityModel: 'default#opaque',
        speedFactor: 2,
        countSides: 16
    });

    ymaps.route(
        [
            [55.7581, 37.61681],
            [55.7171, 37.68681]
        ]
    ).then(
        function (route) {
            var path = route.getPaths().get(0);

            car2.moveOnPath(path).then(function (status) {
                console.log('car 2', status);
            });

        }, function (error) {
            console.error("car 2: " + error.message);
        });

    // Sprite Cars with iconPixelPerfect:true
    var car3 = new GeoMotionObject({
        geometry: {
            type: "Point",
            coordinates: [55.7571, 37.61681]
        }
    }, {
        iconLayout: ymaps.templateLayoutFactory.createClass('<div class="car1 car1_direction_$[properties.direction] car1_state_$[properties.state]"></div>'),
        iconImageSize: [54, 54],
        iconOffset: [-27, -27],
        interactivityModel: 'default#opaque',

        //iconRotation:45,
        speedFactor: 2,
        countSides: 16
    });

    ymaps.route(
        [
            [55.7561, 37.61581],
            [55.7171, 37.68681]
        ]
    ).then(
        function (route) {
            var path = route.getPaths().get(0);

            car3.moveOnPath(path).then(function (status) {
                console.log('car 3', status);
            });

        }, function (error) {
            console.error("car 3: " + error.message);
        });

    var car4 = new GeoMotionObject({
        geometry: {
            type: "Point",
            coordinates: [55.7571, 37.61681]
        }
    }, {
        iconLayout: ymaps.templateLayoutFactory.createClass('<div class="car1 car1_direction_$[properties.direction] car1_state_$[properties.state]"></div>'),
        iconImageSize: [54, 54],
        iconOffset: [-27, -27],
        interactivityModel: 'default#opaque',

        //iconRotation:45,
        speedFactor: 2,
        countSides: 16
    });

    ymaps.route(
        [
            [55.7572, 37.61791],
            [55.7272, 37.68691]
        ]
    ).then(
        function (route) {
            var path = route.getPaths().get(0);

            car4.moveOnPath(path).then(function (status) {
                console.log('car 4', status);
            });

        }, function (error) {
            console.error("car 4: " + error.message);
        });
    var myPlacemark1 = new ymaps.Placemark([55.7571, 37.61681], {iconContent: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#blueSportCircleIcon'}),
        myPlacemark2 = new ymaps.Placemark([55.7581, 37.61681], {iconContent: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#blueSportIcon'}),
        myPlacemark3 = new ymaps.Placemark([55.7591, 37.61681], {iconContent: '22'}, {preset: 'islands#blueIcon'}),
        myPlacemark4 = new ymaps.Placemark([55.7601, 37.61681], {iconContent: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#blueStretchyIcon'}),
        myPlacemark5 = new ymaps.Placemark([55.7611, 37.61681], {iconContent: '22'}, {preset: 'islands#blueDotIcon'}),
        myPlacemark6 = new ymaps.Placemark([55.7621, 37.61681], {iconContent: '22'}, {preset: 'islands#blueCircleIcon'}),
        myPlacemark7 = new ymaps.Placemark([55.7571, 37.61881], {iconContent: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#blueCircleDotIcon'}),
        myPlacemark8 = new ymaps.Placemark([55.7581, 37.61881], {iconCaption: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#blueCircleDotIconWithCaption'}),
        myPlacemark9 = new ymaps.Placemark([55.7591, 37.61881], {iconCaption: 'Очень длиннный, но невероятно интересный текс.'}, {preset: 'islands#blueDotIconWithCaption'}),

        myPlacemark11 = new ymaps.Placemark([55.7601, 37.61881],{iconContent: '22'}),
        myPlacemark10 = new ymaps.Placemark([55.7611, 37.61881], {iconContent: '22'}, {
            preset: 'islands#glyphIcon',
            iconGlyph: "tower",
            iconGlyphColor: 'blue',
            iconColor: 'blue'
        }),
        myPlacemark12 = new ymaps.Placemark([55.7621, 37.61881], {iconContent: '22'}, {
            preset: 'islands#glyphCircleIcon',
            iconGlyph: "tower",
            iconGlyphColor: 'blue',
            iconColor: 'blue'
        }),
        myPlacemark13 = new ymaps.Placemark([55.7631, 37.61881], {iconContent: '22'}, {
            preset: 'islands#blueGlyphIcon',
            iconGlyph: "tower",
            iconGlyphColor: 'blue'
        }),
        collection = new ymaps.GeoObjectCollection();
    collection
        .add(car1)
        .add(car2)
        .add(car3)
        .add(car4)
        .add(myPlacemark1)
        .add(myPlacemark2)
        .add(myPlacemark3)
        .add(myPlacemark4)
        .add(myPlacemark5)
        .add(myPlacemark6)
        .add(myPlacemark7)
        .add(myPlacemark8)
        .add(myPlacemark9)
        .add(myPlacemark10)
        .add(myPlacemark11)
        .add(myPlacemark12)
        .add(myPlacemark13);
    map.geoObjects.add(collection);
    var vector = new ymaps.control.Button("vector");
    var pixelPerfect = new ymaps.control.Button("pixelPerfect");
    map.controls.add(vector, {float: 'right'});
    map.controls.add(pixelPerfect, {float: 'right'});
    pixelPerfect.events
        .add('select', function (e) {
            collection.options.set({iconPixelPerfect:false});
        })
        .add('deselect', function (e) {
            collection.options.set({iconPixelPerfect:true});
        });
    vector.events
        .add('select', function (e) {
            map.options.set({vector:false});
        })
        .add('deselect', function (e) {
            map.options.set({vector:true});
        });
});