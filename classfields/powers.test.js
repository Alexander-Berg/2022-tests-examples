const powers = require('./powers');

const groupingInfo = require('./mocks/groupingInfo.mock.json');

it('возвращает диапазон мощностей', () => {
    expect(powers(groupingInfo)).toEqual('249-333 л.с.');
});

it('powers возвращает пустую строку, если нет техпарамов', () => {
    expect(powers({})).toEqual('');
});
