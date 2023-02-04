const cylinders = require('./cylinders');
const { thinsp } = require('auto-core/react/lib/html-entities');

it('должен вернуть количество цилиндров и расположение', () => {
    expect(cylinders({
        vehicle_info: {
            cylinder_amount: 'CYLINDERS_4',
            cylinder_order: 'V_TYPE',
        },
    })).toEqual(`4${ thinsp }/${ thinsp }V-образное`);
});

it('должен вернуть количество цилиндров с текстом и расположение', () => {
    expect(cylinders({
        vehicle_info: {
            cylinder_amount: 'CYLINDERS_4',
            cylinder_order: 'V_TYPE',
        },
    }, true)).toEqual(`4 цилиндра${ thinsp }/${ thinsp }v-образное`);
});

it('должен вернуть количество цилиндров без расположения, если его нет', () => {
    expect(cylinders({
        vehicle_info: {
            cylinder_amount: 'CYLINDERS_1',
        },
    })).toEqual('1');
});

it('должен вернуть количество цилиндров с текстом, без расположения', () => {
    expect(cylinders({
        vehicle_info: {
            cylinder_amount: 'CYLINDERS_1',
        },
    }, true)).toEqual('1 цилиндр');
});

it('должен вернуть количество цилиндров с текстом (множественное число), без расположения', () => {
    expect(cylinders({
        vehicle_info: {
            cylinder_amount: 'CYLINDERS_8',
        },
    }, true)).toEqual('8 цилиндров');
});
