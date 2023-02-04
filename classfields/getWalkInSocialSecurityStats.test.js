const _ = require('lodash');

const walkInMock = require('auto-core/react/dataDomain/walkIn/mocks/withData.mock');

const getWalkInSocialSecurityStats = require('./getWalkInSocialSecurityStats');

it('должен правильно маппить и селектить группы в гистограммах', () => {
    expect(getWalkInSocialSecurityStats(walkInMock)).toMatchSnapshot();
});

it('должен вырезать UNKNOWN_SEGMENT (Неизвестно), если ни в одной из гистограм нет процентов в этой группе', () => {
    const mockClone = _.cloneDeep(walkInMock);

    mockClone.walkIn.aggregation.female_histogram.groups[5].percent = 0;
    mockClone.walkIn.aggregation.male_histogram.groups[5].percent = 0;

    expect(getWalkInSocialSecurityStats(mockClone)).toMatchSnapshot();
});
