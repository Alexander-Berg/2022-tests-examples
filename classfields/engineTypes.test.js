const engineTypes = require('./engineTypes');

const groupingInfo = require('./mocks/groupingInfo.mock.json');

it('возвращает список двигателей через запятую', () => {
    expect(engineTypes(groupingInfo)).toEqual('бензин, дизель, электро');
});

it('engineTypes возвращает пустую строку, если нет техпарамов', () => {
    expect(engineTypes({})).toEqual('');
});
