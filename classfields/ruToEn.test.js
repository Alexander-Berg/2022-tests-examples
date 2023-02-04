const ruToEn = require('./ruToEn');

it('should convert "фгвш" as "audi"', () => {
    expect(ruToEn('фгвш')).toEqual('audi');
});

it('should convert "Фгвш" as "audi"', () => {
    expect(ruToEn('Фгвш')).toEqual('audi');
});

it('should convert "" as "" (empty value test)', () => {
    expect(ruToEn('')).toEqual('');
});
