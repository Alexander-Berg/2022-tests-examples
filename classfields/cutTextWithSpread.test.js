import cutTextWithSpread from './cutTextWithSpread';

const veryLongString = 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean ma';
const smallString = 'Lorem ipsum dolor sit amet';
const VALID_LENGTH = 100;

it(`возвращает обрезанную строку, если она больше ${ VALID_LENGTH } символов`, () => {
    const cuttedText = cutTextWithSpread(veryLongString, VALID_LENGTH);

    expect(veryLongString).toHaveLength(VALID_LENGTH + 1);
    // Длина может быть меньше, если после ограничения длины в конце остался пробел
    expect(cuttedText).toHaveLength(VALID_LENGTH - 1);
    expect(cuttedText[ cuttedText.length - 1]).toEqual('…');
});

it(`возвращает саму строку, если она меньше ${ VALID_LENGTH } символов`, () => {
    const cuttedText = cutTextWithSpread(smallString, VALID_LENGTH);

    expect(smallString).toHaveLength(26);
    expect(cuttedText).toHaveLength(26);
    expect(cuttedText[ cuttedText.length - 1]).not.toEqual('…');
});
