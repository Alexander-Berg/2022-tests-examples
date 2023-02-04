/**
 * User: agoryunova
 * Date: 11.03.14
 * Time: 15:42
 */

var objectManagerSource = function (ymaps, myMap, that) {

    currentId = 0;

    that.objectPlacemark1 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.711, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
        options: {zIndex: currentId, notOnMap: true, preset: 'islands#blueDotIconWithCaption'}
    };

    that.objectPlacemark2 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.712, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId},
        options: {zIndex: currentId, notOnMap: true, preset: 'islands#blueDotIconWithCaption'}
    };

    that.objectPlacemark3 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.713, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId,
            type: 'кафе'},
        options: {zIndex: currentId, notOnMap: true, preset: 'islands#blueDotIconWithCaption'}
    };

    that.objectPlacemark4 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.714, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId,
            type: 'кафе'},
        options: {zIndex: currentId, notOnMap: true, preset: 'islands#blueDotIconWithCaption'}
    };

    that.objectPlacemark5 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.715, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
        options: {zIndex: currentId, notOnMap: true}
    };

    that.objectPlacemark6 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.716, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId},
        options: {preset: 'islands#yellowIcon', zIndex: currentId}
    };

    that.objectPlacemark7 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.717, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
        options: {preset: 'islands#yellowIcon', zIndex: currentId}
    };

    that.objectPlacemark8 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.718, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
        options: {preset: 'islands#orangeIcon', zIndex: currentId}
    };

    that.objectPlacemark9 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.719, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId},
        options: {preset: 'islands#orangeIcon', zIndex: currentId}
    };

    that.objectPlacemark10 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.720, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'школа', number: 11},
        options: {preset: 'islands#orangeIcon', zIndex: currentId}
    };

    that.objectPlacemark11 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.721, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека', number: 11},
        options: {preset: 'islands#yellowIcon', zIndex: currentId}
    };

    that.objectPlacemark12 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.722, 37.297]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека', number: 11},
        options: {zIndex: currentId}
    };

    that.objectPlacemark13 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.711, 37.2985]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе', number: 11},
        options: {preset: 'islands#orangeIcon', zIndex: currentId}
    };

    that.objectPlacemark14 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.712, 37.2985]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
        options: {zIndex: currentId}
    };

    that.objectPlacemark15 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.713, 37.2985]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
        options: {zIndex: currentId}
    };

    that.objectPlacemark16 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.714, 37.2985]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId},
        options: {zIndex: currentId}
    };

    that.objectPlacemark17 = {
        type: 'Feature',
        id: currentId++,
        geometry: {type: 'Point', coordinates: [55.715, 37.2985]},
        properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
            clusterCaption: 'geoObjectPlacemark balloon' + currentId,
            balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'школа', iconContent: 16},
        options: {preset: 'islands#yellowIcon', zIndex: currentId}
    };

    that.objectManagerArray = [

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.711, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
            options: {preset: 'islands#orangeIcon', zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.712, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
            options: {preset: 'islands#yellowIcon', zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.713, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
            options: {zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.714, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId},
            options: {preset: 'islands#orangeIcon', zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.715, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'id'},
            options: {preset: 'islands#orangeIcon', zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.716, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
            options: {preset: 'islands#yellowIcon', zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.717, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
            options: {zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.718, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
            options: {zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.719, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'школа'},
            options: {zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.720, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId},
            options: {preset: 'islands#yellowIcon', zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.721, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
            options: {preset: 'islands#yellowIcon', zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.722, 37.300]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId},
            options: {preset: 'islands#orangeIcon', zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.711, 37.3015]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'школа'},
            options: {zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.712, 37.3015]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
            options: {zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.713, 37.3015]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
            options: {preset: 'islands#pinkIcon', zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.714, 37.3015]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId},
            options: {preset: 'islands#orangeIcon', zIndex: currentId}},

        {   type: 'Feature',
            id: currentId++,
            geometry: {type: "Point", coordinates: [55.715, 37.3015]},
            properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
            options: {zIndex: currentId}}

    ];


    that.objectManagerCollection = {
        type: 'FeatureCollection',
        features: [
            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.711, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
                options: {preset: 'islands#orangeIcon', zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.712, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'школа'},
                options: {preset: 'islands#yellowIcon', zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.713, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId},
                options: {zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.714, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
                options: {preset: 'islands#yellowIcon', zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.715, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
                options: {zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.716, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
                options: {preset: 'islands#orangeIcon', zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.717, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
                options: {zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.718, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId},
                options: {preset: 'islands#orangeIcon', zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.719, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId},
                options: {zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.720, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
                options: {zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.721, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
                options: {zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.722, 37.3035]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId},
                options: {preset: 'islands#yellowIcon', zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.711, 37.305]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'школа'},
                options: {preset: 'islands#pinkIcon', zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.712, 37.305]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'школа'},
                options: {preset: 'islands#orangeIcon', zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.713, 37.305]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId},
                options: {preset: 'islands#yellowIcon', zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.714, 37.305]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'кафе'},
                options: {preset: 'islands#yellowIcon', zIndex: currentId}},

            {   type: 'Feature',
                id: currentId++,
                geometry: {type: "Point", coordinates: [55.715, 37.305]},
                properties: {hintContent: 'geoObjectPlacemark hint' + currentId,
                    clusterCaption: 'geoObjectPlacemark balloon' + currentId,
                    balloonContent: 'geoObjectPlacemark balloon' + currentId, type: 'аптека'},
                options: {preset: 'islands#yellowIcon', zIndex: currentId}}

        ]
    };

    that.objectManagerArrayNonPointObjects = [
        {
            type: 'Feature',
            id: currentId++,
            geometry: { type: "Polygon", coordinates: [
                [
                    [55.71135399209327, 37.30620048320219],
                    [55.71290459171496, 37.30611465251371],
                    [55.7128803640712, 37.308818319200725],
                    [55.711426677831255, 37.30886123454496],
                    [55.71135399209327, 37.30620048320219]
                ],
                [
                    [55.711426677831255, 37.30886123454496],
                    [55.7128803640712, 37.31023452556058],
                    [55.71137822068769, 37.31156490123197],
                    [55.711426677831255, 37.30886123454496]
                ]
            ] },
            properties: {
                hintContent: 'objectManagerPolygon' + currentId,
                balloonContent: 'objectManagerPolygon' + currentId, type: 'кафе'
            }, options: {preset: 'islands#yellowIcon', fillColor: '#ffff00'}},
        {
            type: 'Feature',
            id: currentId++,
            geometry: { type: "LineString", coordinates: [
                [55.71765291857969, 37.30615756785795],
                [55.71997841821334, 37.308818319200725],
                [55.71767714325092, 37.311607816576206]
            ] },
            properties: {
                hintContent: 'objectManagerLineString' + currentId,
                balloonContent: 'objectManagerLineString' + currentId, type: 'школа'
            }, options: {}
        },
        {
            type: 'Feature',
            id: currentId++,
            geometry: { type: "Circle", coordinates: [55.717, 37.3089041498892], radius: 100 },
            properties: {
                hintContent: 'objectManagerCircle' + currentId,
                balloonContent: 'objectManagerCircle' + currentId, type: 'аптека'
            }, options: {}
        },
        {
            type: 'Feature',
            id: currentId++,
            geometry: { type: "Rectangle", coordinates: [
                [55.71302572970743, 37.30611465251371],
                [55.714527809473324, 37.311650731920444]
            ] },
            properties: {
                hintContent: 'objectManagerRectangle' + currentId,
                balloonContent: 'objectManagerRectangle' + currentId, type: 'id'
            }, options: {}
        }
    ];

    that.objectManagerCollectionNonPointObjects = {
        type: 'FeatureCollection',
        features: [
            {
                type: 'Feature',
                id: currentId++,
                geometry: { type: "Polygon", coordinates: [
                    [
                        [55.71135399209327, 37.28620048320219],
                        [55.71290459171496, 37.28611465251371],
                        [55.7128803640712, 37.288818319200725],
                        [55.711426677831255, 37.28886123454496],
                        [55.71135399209327, 37.28620048320219]
                    ],
                    [
                        [55.711426677831255, 37.28886123454496],
                        [55.7128803640712, 37.29023452556058],
                        [55.71137822068769, 37.29156490123197],
                        [55.711426677831255, 37.28886123454496]
                    ]
                ] },
                properties: {
                    hintContent: 'objectManagerPolygon' + currentId,
                    balloonContent: 'objectManagerPolygon' + currentId, type: 'кафе'
                }, options: {}},
            {
                type: 'Feature',
                id: currentId++,
                geometry: { type: "LineString", coordinates: [
                    [55.71765291857969, 37.28615756785795],
                    [55.71997841821334, 37.288818319200725],
                    [55.71767714325092, 37.291607816576206]
                ] },
                properties: {
                    hintContent: 'objectManagerLineString' + currentId,
                    balloonContent: 'objectManagerLineString' + currentId
                }, options: {}
            },
            {
                type: 'Feature',
                id: currentId++,
                geometry: { type: "Circle", coordinates: [55.717, 37.2889041498892], radius: 100 },
                properties: {
                    hintContent: 'objectManagerCircle' + currentId,
                    balloonContent: 'objectManagerCircle' + currentId
                }, options: {}
            },
            {
                type: 'Feature',
                id: currentId++,
                geometry: { type: "Rectangle", coordinates: [
                    [55.71302572970743, 37.28611465251371],
                    [55.714527809473324, 37.291650731920444]
                ] },
                properties: {
                    hintContent: 'objectManagerRectangle' + currentId,
                    balloonContent: 'objectManagerRectangle' + currentId
                }, options: {}
            }
        ]
    };

    that.objectManagerPolygon = {
        type: 'Feature',
        id: currentId++,
        geometry: { type: "Polygon", coordinates: [
            [
                [55.71135399209327, 37.29620048320219],
                [55.71290459171496, 37.29611465251371],
                [55.7128803640712, 37.298818319200725],
                [55.711426677831255, 37.29886123454496],
                [55.71135399209327, 37.29620048320219]
            ],
            [
                [55.711426677831255, 37.29886123454496],
                [55.7128803640712, 37.30023452556058],
                [55.71137822068769, 37.30156490123197],
                [55.711426677831255, 37.29886123454496]
            ]
        ] },
        properties: {
            hintContent: 'objectManagerPolygon' + currentId,
            balloonContent: 'objectManagerPolygon' + currentId, type: 'школа'
        }, options: {}
    };
    that.objectManagerPolyline = {
        type: 'Feature',
        id: currentId++,
        geometry: { type: "LineString", coordinates: [
            [55.71765291857969, 37.29615756785795],
            [55.71997841821334, 37.298818319200725],
            [55.71767714325092, 37.301607816576206]
        ] },
        properties: {
            hintContent: 'objectManagerLineString' + currentId,
            balloonContent: 'objectManagerLineString' + currentId
        }, options: {}
    };
    that.objectManagerCircle = {
        type: 'Feature',
        id: currentId++,
        geometry: { type: "Circle", coordinates: [55.71607828258542, 37.2989041498892], radius: 100 },
        properties: {
            hintContent: 'objectManagerCircle' + currentId,
            balloonContent: 'objectManagerCircle' + currentId
        }, options: {}
    };
    that.objectManagerRectangle = {
        type: 'Feature',
        id: currentId++,
        geometry: { type: "Rectangle", coordinates: [
            [55.71302572970743, 37.29611465251371],
            [55.714527809473324, 37.301650731920444]
        ] },
        properties: {
            hintContent: 'objectManagerRectangle' + currentId,
            balloonContent: 'objectManagerRectangle' + currentId
        }, options: {}
    };
};