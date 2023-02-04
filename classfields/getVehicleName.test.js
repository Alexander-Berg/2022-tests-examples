const getVehicleName = require('./getVehicleName');

it('должен вернуть название без configuration.notice, если его нет', () => {
    const name = getVehicleName({
        mark: {
            name: 'BMW',
        },
        model: {
            name: '3 серия',
        },
        generation: {
            name: 'VI (F3x)',
        },
    });

    expect(name).toEqual('BMW 3 серия VI (F3x)');
});
