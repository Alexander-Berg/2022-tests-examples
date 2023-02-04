const complectationNames = require('./complectationNames');

const groupingInfo = require('./mocks/groupingInfo.mock.json');

it('возвращает уникальные названия комплектаций', () => {
    expect(complectationNames(groupingInfo)).toEqual([ 'xDrive20i M Sport', 'xDrive20i Advantage' ]);
});

it('возвращает пустой массив, если нет комплектаций', () => {
    expect(complectationNames({})).toEqual([]);
});
