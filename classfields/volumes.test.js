const volumes = require('./volumes');

const groupingInfo = require('./mocks/groupingInfo.mock.json');

it('возвращает диапазон объемов', () => {
    expect(volumes(groupingInfo)).toEqual('2.0, 3.0 л');
});

it('volumes возвращает пустую строку, если нет техпарамов', () => {
    expect(volumes({})).toEqual('');
});
