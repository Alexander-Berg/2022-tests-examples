const prepareSize = require('./prepareSize');

it('должен вырезать units для размера колес', () => {
    const item = {
        id: 'wheel_size',
        units: 'мм',
    };

    const expected = {
        id: 'wheel_size',
    };

    expect(prepareSize(item)).toEqual(expected);
});
