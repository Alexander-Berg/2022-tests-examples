const getDisplacementRange = require('./getDisplacementRange');

it('формирует диапазон объёма двигателя для 0 (электрокары)', () => {
    const range = getDisplacementRange(0);

    expect(range).toEqual({
        displacement_from: null,
        displacement_to: null,
    });
});

it('формирует диапазон объёма двигателя для undefined', () => {
    const range = getDisplacementRange();

    expect(range).toEqual({
        displacement_from: null,
        displacement_to: null,
    });
});

it('формирует диапазон объёма двигателя для числа', () => {
    const range = getDisplacementRange(200);

    expect(range).toEqual({
        displacement_from: 200,
        displacement_to: 200,
    });
});
