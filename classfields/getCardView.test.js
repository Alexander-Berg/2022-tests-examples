const dashboardCardViewMock = require('../mocks/withOfferDailyStats.mock');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const getCardView = require('./getCardView');

const state = {
    dashboard: dashboardCardViewMock,
    bunker: getBunkerMock([ 'cabinet/dashboard' ]),
};

it(`должен отдать для графика вторую половину переданных элементов`, () => {
    const result = getCardView(state);

    expect(result.chartData).toHaveLength(2);
    expect(result.chartData).toMatchSnapshot();
});
