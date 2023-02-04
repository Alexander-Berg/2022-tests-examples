import gearType from './gearType';

it('должен правильно отдать один привод', () => {
    expect(gearType.get('ALL_WHEEL_DRIVE')).toBe('полный');
});

it('должен правильно отдать несколько привод', () => {
    expect(gearType.get([ 'ALL_WHEEL_DRIVE', 'FORWARD_CONTROL' ])).toBe('полный, передний');
});
