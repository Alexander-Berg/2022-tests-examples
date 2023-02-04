const complectationsCount = require('./complectationsCount');

const groupingInfo = require('./mocks/groupingInfo.mock.json');

it('возвращает количество комплектаций', () => {
    expect(complectationsCount(groupingInfo)).toEqual(2);
});

it('возвращает 0, если нет комплектаций', () => {
    expect(complectationsCount({})).toEqual(0);
});
