const gearTypes = require('./gearTypes');

const groupingInfo = require('./mocks/groupingInfo.mock.json');

it('возвращает список приводов через запятую', () => {
    expect(gearTypes(groupingInfo)).toEqual('полный, передний, задний');
});

it('возвращает пустую строку, если нет техпарамов', () => {
    expect(gearTypes({})).toEqual('');
});
