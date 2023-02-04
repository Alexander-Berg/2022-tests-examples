const MockDate = require('mockdate');
const _ = require('lodash');

const walkInMock = require('auto-core/react/dataDomain/walkIn/mocks/withData.mock');

const getWalkInTotalStats = require('./getWalkInTotalStats');

beforeEach(() => {
    MockDate.set('2019-01-05');
});

afterEach(() => {
    MockDate.reset();
});

it('должен обогащать статистику визитов лейблами, именами и цветами', () => {
    expect(getWalkInTotalStats(walkInMock)).toMatchSnapshot();
});

it('должен изменить элементы для текущего и предыдущего дня, если там нет данных', () => {
    const walkInMockClone = _.cloneDeep(walkInMock);
    walkInMockClone.walkIn.aggregation.daily_stats.points[1].date = '2019-01-05';
    walkInMockClone.walkIn.aggregation.daily_stats.points[1].visits = {};

    expect(getWalkInTotalStats(walkInMockClone)).toMatchSnapshot();
});
