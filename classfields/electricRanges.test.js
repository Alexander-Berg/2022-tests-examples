const { ndash } = require('auto-core/react/lib/html-entities');

const electricRanges = require('./electricRanges');

const groupingInfo = require('./mocks/electroCarGroupingInfo.mock.json');

it('возвращает диапазон мощностей', () => {
    expect(electricRanges(groupingInfo)).toEqual(`Заряд на 200 ${ ndash } 300 км`);
});

it('powers возвращает пустую строку, если нет техпарамов', () => {
    expect(electricRanges({})).toEqual('');
});
