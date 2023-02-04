const transmissions = require('./transmissions');

const groupingInfo = require('./mocks/groupingInfo.mock.json');

it('возвращает список типов трансмиссии через запятую', () => {
    expect(transmissions(groupingInfo)).toEqual('автомат, робот, типтроник, механика');
});

it('возвращает пустую строку, если нет техпарамов', () => {
    expect(transmissions({})).toEqual('');
});
