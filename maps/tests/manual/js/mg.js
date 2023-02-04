var geoCSSource = function (ymaps, myMap, that) {

    that.multiPlacemark = new ymaps.GeoObject({
        geometry: {
            type: "MultiPoint", coordinates: [
                [55.81, 37.70],
                [55.82, 37.70],
                [55.83, 37.70],
                [55.84, 37.70],
                [55.85, 37.70]
            ]
        }, properties: {custom: true}
    }, {id: 'geoObjectPlacemark', preset: 'islands#greenIcon'});

    that.multiPolyline = new ymaps.GeoObject({
        geometry: {
            type: "MultiLineString", coordinates: [
                [

                        [56.787092634473694, 37.667663574218096], [56.68797724922255, 37.666976928710035], [56.68754896639797, 37.57658996581947], [56.78474860376539, 37.57684020996037]

                ],
                [

                        [56.81, 37.60],
                        [56.71, 37.40]

                ],
                [

                        [56.80, 37.50],
                        [56.80, 37.40],
                        [56.70, 37.50]

                ]
            ]
        }, properties: {custom: false}
    }, {strokeWidth: 3, id: 'geoObjectPolyline', strokeColor: '#00FF00'});

    that.multiPolygon = new ymaps.GeoObject({
        geometry: {
            type: "MultiPolygon", coordinates: [
                [
                [
                    [55.787092634473694, 37.667663574218096], [55.68797724922255, 37.666976928710035], [55.68754896639797, 37.57658996581947], [55.78474860376539, 37.57684020996037], [55.787092634473694, 37.667663574218096]
                ],
                [
                    [55.78709263446105, 37.71984863281182], [55.68797724922255, 37.666976928710035], [55.68599735621169, 37.78258361816322], [55.78709263446105, 37.71984863281182]
                ]
            ],
                [
                    [
                        [55.81, 37.60],
                        [55.71, 37.40],
                        [55.71, 37.60]
                    ]
                ],
                [
                    [
                        [55.80, 37.50],
                        [55.80, 37.40],
                        [55.70, 37.50],
                        [55.70, 37.40]
                    ]
                ]
            ]
        }, properties: {custom: false}
    }, {id: 'geoObjectPolygon', strokeColor: '#AA0000', fillColor: '#00FF00'/*, fillOpacity: 0.5*/});
};