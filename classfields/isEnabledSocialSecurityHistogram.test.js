const _ = require('lodash');
const walkInMock = require('auto-core/react/dataDomain/walkIn/mocks/withData.mock');

const isEnabledSocialSecurityHistogram = require('./isEnabledSocialSecurityHistogram');

let state;

beforeEach(() => {
    state = _.cloneDeep(walkInMock);
});

it('должен отдавать true, если в гистограммах есть каунтеры', () => {
    expect(isEnabledSocialSecurityHistogram(state)).toBe(true);
});

it('должен отдавать false, если нет каунтеров совсем', () => {
    state.walkIn.aggregation.male_histogram.total_count = 0;
    state.walkIn.aggregation.female_histogram.total_count = 0;

    expect(isEnabledSocialSecurityHistogram(state)).toBe(false);
});
