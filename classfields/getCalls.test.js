jest.mock('www-cabinet/react/dataDomain/dashboard/helpers/calculateCallsChartData', () => {
    return () => 'CHART_DATA';
});

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const statsCallsDailyListMock = require('../mocks/withStatsCallsDailyList.mock');

const getCalls = require('./getCalls');

const state = {
    dashboard: {
        dateLimits: { from: '2019-10-11', to: '2019-10-14' },
        statsCallsDailyList: { calls_by_day: statsCallsDailyListMock },
    },
    bunker: getBunkerMock([ 'cabinet/dashboard' ]),
    config: { client: { salon: { poi: { call_tracking_on: '1' } } } },
};

it(`должен отдать правильную инфу о звонках`, () => {
    const result = getCalls(state);

    expect(result).toMatchSnapshot();
});
