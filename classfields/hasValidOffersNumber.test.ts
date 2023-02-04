import hasValidOffersNumber from './hasValidOffersNumber';

it('валидное количестко офферов на листинге', () => {
    const result = hasValidOffersNumber(3);
    expect(result).toEqual(true);
});

it('невалидное количестко офферов на листинге', () => {
    const result = hasValidOffersNumber(2);
    expect(result).toEqual(false);
});
