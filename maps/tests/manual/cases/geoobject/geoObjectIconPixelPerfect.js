/* global ymaps, GeoMotionObject */

Api('init');
function  init () {
    "use strict";

    var ymaps = ym; // hack
    var map = new ymaps.Map("map", {
        center: [55.7571, 37.61681], // ~msk
        zoom: 15,
        controls: []
    },{
        geoObjectIconPixelPerfect:false
    });

    // ============= Create cars

    // Sprite Car
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

        //iconRotation:45,

        // ����������
        speedFactor: 2,
        // ���������� �������������� ������ (4/8/16)
        countSides: 16
    });

    // Transform Car
    var car2 = new GeoMotionObject({
        // ��������� ��������� ���� "�����".
        geometry: {
            type: "Point",
            coordinates: [55.7571, 37.61681]
        }
    }, {
        iconLayout: ymaps.templateLayoutFactory.createClass('<div class="car2 car2_direction_$[properties.direction] car2_state_$[properties.state]" style="-webkit-transform: rotate($[properties.deg]deg); -ms-transform: rotate($[properties.deg]deg); transform: rotate($[properties.deg]deg);"></div>'),
        iconImageSize: [54, 54],
        iconOffset: [-27, -27],
        interactivityModel: 'default#opaque',

      //  iconRotation:100,

        // ����������
        speedFactor: 4,
        // ���������� �������������� ������ (4/8/16)
        countSides: 16
    });

    //
    var girl = new GeoMotionObject({
        // ��������� ��������� ���� "�����".
        geometry: {
            type: "Point",
            coordinates: [55.7571, 37.61681]
        }
    }, {
        iconLayout: ymaps.templateLayoutFactory.createClass('<div class="girl girl_direction_$[properties.direction]"></div>'),
        iconImageSize: [48, 48],
        iconOffset: [-24, -24],
        interactivityModel: 'default#opaque',

        // ����������
        speedFactor: 1,
        // ���������� �������������� ������ (4/8/16)
        countSides: 4,
        // ����� ����������� ����� ��������
        needAnimationTimeout: true
    });

    var play = new ymaps.control.Button("Pause");
    var abort = new ymaps.control.Button("Abort");
    map.controls.add(play, {float: 'right'});
    map.controls.add(abort, {float: 'right'});

    ymaps.route(
        [
            [55.7571, 37.61681],
            [55.7171, 37.68681] // � �� ����� "�������������"
        ]
    ).then(
        function (route) {
            var path = route.getPaths().get(0);

            map.geoObjects.add(car1);

            car1.moveOnPath(path).then(function (status) {
                console.log('car 1', status);
            });

        }, function (error) {
            console.error("�������� ������: " + error.message);
        });

    ymaps.route(
        [
            [55.7571, 37.61681],
            [55.7871, 37.58681],
            [55.7971, 37.64681],
            [55.7271, 37.70681]
        ]
    ).then(
        function (route) {
            // ������� �������� ����� � ��������� � �������� ������
            var points = route.getWayPoints();
            points.get(0).properties.set("iconContent", "�");
            points.get(1).properties.set("iconContent", "�");

            var paths = route.getPaths();

            route.getPaths().options.set({
                strokeColor: '110000ff',
                opacity: 0.4
            });

            map.geoObjects.add(route);
            map.geoObjects.add(car2);

            car2.moveOnRoute(paths).then(function (status) {
                console.log('car 2', status);
            });

        }, function (error) {
            console.error("�������� ������: " + error.message);
        });

    var girlRoutePoints = [
        [55.7571, 37.61681],
        [55.7571, 37.66681],
        [55.7571, 37.66681],
        [55.7171, 37.66681],
        [55.7171, 37.66681],
        [55.7271, 37.60681],
        [55.7271, 37.60681],
        [55.7871, 37.60681]
    ];
    var girlRoute = new ymaps.Polyline(girlRoutePoints, {
    }, {
        strokeColor: '#ff9baa',
        strokeWidth: 4
    });

    // ��������� ����� �� �����.
    map.geoObjects.add(girlRoute);
    map.geoObjects.add(girl);

    girl.moveOnPoints(girlRoutePoints, {time: 60, distance: girlRoute.geometry.getDistance()}).then(function (status) {
        console.log('girl', status);
    }, function (er) {
        console.log('girl err', er);
    });

    play.events
        .add('select', function (e) {
            play.data.set('content', 'Play');

            car1.pause();
            car2.pause();
            girl.pause();
        })
        .add('deselect', function (e) {
            play.data.set('content', 'Pause');

            car1.resume();
            car2.resume();
            girl.resume();
        });

    abort.events
        .add('select', function (e) {
            car1.abort();
            car2.abort();
            girl.abort();
        });
}
