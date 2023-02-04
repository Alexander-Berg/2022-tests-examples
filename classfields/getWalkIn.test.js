const MockDate = require('mockdate');

const dashboardMock = require('../mocks/withWalkIn.mock');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const getWalkIn = require('./getWalkIn');

const state = {
    dashboard: dashboardMock,
    bunker: getBunkerMock([ 'cabinet/dashboard' ]),
};

beforeEach(() => {
    MockDate.set('2018-10-11');
});

afterEach(() => {
    MockDate.reset();
});

it(`должен вырезать элемент с сегодняшней датой из chartData`, () => {
    MockDate.set('2019-10-14');

    const result = getWalkIn(state);

    expect(result.chartData).toHaveLength(1);
});

it(`должен маппить эвенты в формат для графика, считать тоталы и обогащать информацией из бункера`, () => {
    const result = getWalkIn(state);

    expect(result).toMatchSnapshot();
});

it(`должен проставлять hasDataFromBackend в false, если нет данных`, () => {
    const newState = { ...state, dashboard: { walkInAggregation: {} } };
    const result = getWalkIn(newState);

    expect(result.hasDataFromBackend).toBe(false);
});
