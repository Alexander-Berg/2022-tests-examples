const prepareDealersParams = require('./prepareDealersParams');

it('функция не должна удалить марку машины', () => {
    const params = {
        category: 'cars',
        section: 'new',
        mark: 'ford',
        model: 'mustang',
    };

    const context = {
        req: {
            geoIdsInfo: [],
            regionByIp: {
                id: 10000,
                type: 0,
                parent_id: 0,
                name: 'Земля',
                latitude: 0,
                longitude: 0,
            },
            gradius: {
                reqData: {
                    geoIds: [],
                    geoIdsInfo: [],
                    geoIdsParents: {},
                    routerParams: {
                        category: 'cars',
                        mark: 'ford',
                        model: 'focus',
                        specification: 'harakteristiki-dvigatelya',
                    },
                },
                get: () => {},
            },
            susanin: {
                getRouteByName: () => {},
            },
        },
    };

    const testResult = prepareDealersParams({ context, params });

    expect(testResult).toMatchSnapshot();
});
