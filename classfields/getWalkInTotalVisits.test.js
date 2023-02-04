const walkInMock = require('auto-core/react/dataDomain/walkIn/mocks/withData.mock');

const getWalkInTotalVisits = require('./getWalkInTotalVisits');

it('должен отдавать правильный каунтер', () => {
    expect(getWalkInTotalVisits(walkInMock)).toEqual({ confirmed_visits: 460, unconfirmed_visits: 736 });
});
