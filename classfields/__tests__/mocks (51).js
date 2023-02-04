/* eslint-disable max-len */
module.exports = {
    card: {
        buildingFeatures: {
            wallTypes: [ {
                text: 'Долговечный материал, позволяет приступить к отделочным работам почти сразу же после завершения строительства.',
                type: 'MONOLIT'
            } ],
            state: 'HAND_OVER'
        },
        transactionTerms: {
            agreementType: 'DDU'
        },
        location: {
            point: {
                latitude: '123',
                longitude: '123',
                precision: 'EXACT'
            },
            geoId: 213
        },
        price: {
            from: '123'
        },
        fullName: 'ЖК Небо',
        id: 932106,
        developer: {
            id: 268706
        }
    },
    housing: [
        {
            id: 'finished',
            color: '#3bbd25',
            polygons: [
                {
                    id: '123',
                    latitudes: [ '123' ],
                    longitudes: [ '123' ]
                }
            ]
        }
    ],
    crc: 'yfcc6f8efcd833a3c2584eeefe4a920db',
    layersFromShortcuts: [ 'location', 'panorama', 'progress', 'price-rent', 'price-sell', 'profitability', 'transport', 'carsharing' ],
    state: {
        map: {}
    }
};
