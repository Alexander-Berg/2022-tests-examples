const getCylinderAmount = require('./getCylinderAmount');

it('должен вернуть 4 для CYLINDERS_4', () => {
    expect(getCylinderAmount({
        vehicle_info: {
            cylinder_amount: 'CYLINDERS_4',
        },
    })).toEqual(4);
});

it('должен вернуть null, если нет информации', () => {
    expect(getCylinderAmount({
        vehicle_info: {},
    })).toBeNull();
});
