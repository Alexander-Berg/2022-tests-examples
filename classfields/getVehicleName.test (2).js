const getVehicleName = require('./getVehicleName');

it('должен вернуть название без configuration.notice, если его нет', () => {
    const name = getVehicleName({
        vehicle_info: {
            mark_info: { name: 'BMW' },
            model_info: { name: '3 серия' },
            super_gen: { id: '1', name: 'VI (F3x)' },
        },
    });

    expect(name).toEqual('BMW 3 серия VI (F3x)');
});

it('должен вернуть название с configuration.notice, если оно не совпадает с nameplate.name', () => {
    const name = getVehicleName({
        vehicle_info: {
            mark_info: { name: 'BMW' },
            model_info: { name: '3 серия' },
            super_gen: { id: '1', name: 'VI (F3x)' },
            configuration: { notice: 'Gran Turismo' },
            tech_param: { nameplate: '320d xDrive' },
        },
    });

    expect(name).toEqual('BMW 3 серия Gran Turismo 320d xDrive VI (F3x)');
});

it('не должен вернуть название с configuration.notice, если оно совпадает с nameplate.name', () => {
    const name = getVehicleName({
        vehicle_info: {
            mark_info: { name: 'Chrysler' },
            model_info: { name: 'Voyager' },
            super_gen: { id: '1', name: 'IV' },
            configuration: { notice: 'Grand' },
            tech_param: { nameplate: 'Grand' },
        },
    });

    expect(name).toEqual('Chrysler Voyager Grand IV');
});

it('не должен вернуть название с configuration.notice, если оно включено с nameplate.name', () => {
    const name = getVehicleName({
        vehicle_info: {
            mark_info: { name: 'Chrysler' },
            model_info: { name: 'Voyager' },
            super_gen: { id: '1', name: 'IV' },
            configuration: { notice: 'Grand' },
            tech_param: { nameplate: 'Grand Turismo' },
        },
    });

    expect(name).toEqual('Chrysler Voyager Grand Turismo IV');
});

it('должен вернуть название с configuration.notice, если оно совпадает с nameplate.name и передан флаг hideNameplate', () => {
    const name = getVehicleName({
        vehicle_info: {
            mark_info: { name: 'Chrysler' },
            model_info: { name: 'Voyager' },
            super_gen: { id: '1', name: 'IV' },
            configuration: { notice: 'Grand' },
            tech_param: { nameplate: 'Grand' },
        },
    }, { hideNameplate: true });

    expect(name).toEqual('Chrysler Voyager Grand IV');
});
