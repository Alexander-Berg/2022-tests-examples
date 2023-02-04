const _ = require('lodash');
const getFeatures = require('./getFeatures');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const withApplyMock = require('../mocks/withApply.mock');

const initialState = {
    bunker: getBunkerMock([ 'cabinet/loyalty' ]),
};

it('должен выбирать не региональные фичи', () => {
    const loyaltyMock = _.cloneDeep(withApplyMock);

    loyaltyMock.report.tags = [ 'pay_cashback_once_for_non_regional_client' ];

    const state = {
        ...initialState,
        loyalty: loyaltyMock,
    };

    const result = getFeatures(state);

    const expectedDict = initialState.bunker['cabinet/loyalty'].features['non-regional'];

    expect(result).toEqual(expectedDict);
});

it('должен выбирать региональные фичи', () => {
    const loyaltyMock = _.cloneDeep(withApplyMock);

    loyaltyMock.report.tags = [];

    const state = {
        ...initialState,
        loyalty: loyaltyMock,
    };

    const result = getFeatures(state);

    const expectedDict = initialState.bunker['cabinet/loyalty'].features.regional;

    expect(result).toEqual(expectedDict);
});
