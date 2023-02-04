const mileage = require('./mileage');
const { nbsp } = require('auto-core/react/lib/html-entities');

it('должен вернуть пробег в км, если это не спецтехника', () => {
    const offer = {
        state: { mileage: 100500 },
        vehicle_info: { operating_hours: 123456 },
    };
    expect(mileage(offer)).toBe(`100${ nbsp }500${ nbsp }км`);
});

it('должен вернуть пробег в моточасах, если это спецтехника', () => {
    const offer = {
        state: { mileage: 100500 },
        vehicle_info: { operating_hours: 123456, truck_category: 'CRANE' },
    };
    expect(mileage(offer)).toBe(`123${ nbsp }456${ nbsp }моточасов`);
});

it('должен вернуть пробег в моточасах с пробелом вместо nbsp', () => {
    const offer = {
        state: { mileage: 100500 },
        vehicle_info: { operating_hours: 123456, truck_category: 'CRANE' },
    };
    expect(mileage(offer, true)).toBe(`123${ nbsp }456 моточасов`);
});

it('должен вернуть "новый", если это моточасы и spaceBeforeDimension=true', () => {
    const offer = {
        section: 'new',
        vehicle_info: { operating_hours: 0, truck_category: 'CRANE' },
    };
    expect(mileage(offer, true)).toBe(`новый`);
});

it('не должен вернуть пробег, если это новый тачка', () => {
    const offer = {
        section: 'new',
        state: { mileage: 0 },
    };
    expect(mileage(offer)).toBe(`новый`);
});

it('не должен вернуть пробег, если это новый спецтехника', () => {
    const offer = {
        section: 'new',
        vehicle_info: { operating_hours: 0, truck_category: 'CRANE' },
    };
    expect(mileage(offer)).toBe(`новый`);
});

it('не должен вернуть пробег, если нет пробега', () => {
    const offer = {
        state: {},
        vehicle_info: { operating_hours: 123456 },
    };
    expect(mileage(offer)).toBe(`пробег не указан`);
});

it('должен вернуть пробег, если это спецтехника и нет моточасов', () => {
    const offer = {
        state: { mileage: 100500 },
        vehicle_info: { truck_category: 'CRANE' },
    };
    expect(mileage(offer)).toBe(`пробег не указан`);
});
