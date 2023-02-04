const getPurchaseDateText = require('./getPurchaseDateText');

it('должен вернуть null, если не знаем дату покупки', () => {
    expect(getPurchaseDateText()).toBeNull();
});

it('должен вернуть дату покупки', () => {
    expect(getPurchaseDateText('1102712400000')).toBe('С декабря 2004 г.');
});
