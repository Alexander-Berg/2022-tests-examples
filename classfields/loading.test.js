const loading = require('./loading');
const { nbsp } = require('auto-core/react/lib/html-entities');

it('должен вернуть грузоподъемность в кг', () => {
    const offer = { vehicle_info: { loading: 900 } };
    expect(loading(offer)).toBe(`0.90${ nbsp }т`);
});

it('должен вернуть грузоподъемность в тоннах', () => {
    const offer = { vehicle_info: { loading: 1000 } };
    expect(loading(offer)).toBe(`1.0${ nbsp }т`);
});
