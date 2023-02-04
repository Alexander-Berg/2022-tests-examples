import formatCardName from './formatCardName';

it('должен отформатировать штатно', () => {
    const result = formatCardName('111||1234', 'MASTERCARD');
    const expected = 'MasterCard **** 1234';
    expect(result).toEqual(expected);
});

it('должен отформатировать нештатный бренд', () => {
    const result = formatCardName('111||1234', 'MASTERCARD1');
    const expected = 'MASTERCARD1 **** 1234';
    expect(result).toEqual(expected);
});
