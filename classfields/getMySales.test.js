const _ = require('lodash');
const dashboardMock = require('../mocks/withCounters.mock');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const getMySales = require('./getMySales');

const state = {
    dashboard: dashboardMock,
    bunker: getBunkerMock([ 'cabinet/dashboard' ]),
};

it(`должен правильно посчитать и поселектить каунтеры`, () => {
    const result = getMySales(state);

    const counters = _.pick(result, [ 'total', 'chartData' ]);

    expect(counters).toMatchSnapshot();
});

it(`должен вернуть hasDataFromBackend === false, если _.isEmpty(userOfferCounters)`, () => {
    const result = getMySales({
        dashboard: undefined,
        bunker: getBunkerMock([ 'cabinet/dashboard' ]),
    });

    expect(result.hasDataFromBackend).toBe(false);
});

it(`должен вернуть hasDataFromBackend === false, если userOfferCounters.all.error`, () => {
    const result = getMySales({
        dashboard: {
            userOfferCounters: {
                all: {
                    error: true,
                },
            },
        },
        bunker: getBunkerMock([ 'cabinet/dashboard' ]),
    });

    expect(result.hasDataFromBackend).toBe(false);
});
