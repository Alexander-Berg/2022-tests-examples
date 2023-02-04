const getDescription = require('./getDescription');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const withApplyMock = require('../mocks/withApply.mock');
const withoutApplyMock = require('../mocks/withoutApply.mock');

const initialState = {
    bunker: getBunkerMock([ 'cabinet/loyalty' ]),
};

it('должен выбирать словарь описаний для пройденного апрува', () => {
    const state = {
        ...initialState,
        loyalty: withApplyMock,
    };

    const result = getDescription(state);

    const expectedDict = initialState.bunker['cabinet/loyalty'].passed;

    expect(result).toEqual(expectedDict);
});

it('должен выбирать словарь описаний для не пройденного апрува', () => {
    const state = {
        ...initialState,
        loyalty: withoutApplyMock,
    };

    const result = getDescription(state);

    const expectedDict = initialState.bunker['cabinet/loyalty'].failed;

    expect(result).toEqual(expectedDict);
});
