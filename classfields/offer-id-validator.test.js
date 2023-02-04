const offerIdValidator = require('./offer-id-validator');

it('Должен распознать оффер-айди', () => {
    expect(offerIdValidator('1092321134-b575fe97')).toBe(true);
});

it('Должен сказать, что строка не является оффер-айди', () => {
    expect(offerIdValidator('1092321134')).toBe(false);
});

it('Должен сказать, что строка не является оффер-айди 2', () => {
    expect(offerIdValidator('1092321134-123-456')).toBe(false);
});
