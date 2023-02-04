const getColors = require('./getColors');

const COLORS_HEX = [ '040001', 'CACECB', 'FAFBFB', '97948F' ];

const COMPLECTATION = {
    colors_hex: COLORS_HEX,
};

it('должен правильно вернуть массив цветов', () => {
    expect(getColors(COMPLECTATION)).toEqual(COLORS_HEX);
});

it('должен вернуть пустой массив если нет цветов', () => {
    expect(getColors({})).toEqual([]);
});
