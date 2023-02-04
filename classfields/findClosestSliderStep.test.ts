import findClosestSliderStep from './findClosestSliderStep';

const testAmountValues = [
    { value: 27100 },
    { value: 31400 },
    { value: 44300 },
    { value: 51850 },
    { value: 64000 },
];

const testTermValues = [
    { value: 1 },
    { value: 2 },
    { value: 3 },
    { value: 5 },
];

it('возвращает верное значение для теста на суммах', () => {
    expect(findClosestSliderStep({ searched: 47000, values: testAmountValues })).toEqual(44300);
});

it('возвращает наибольшее из ближайших значений', () => {
    expect(findClosestSliderStep({ searched: 4, values: testTermValues })).toEqual(5);
});
