import { getSitePopulatedRgid } from '../index';

import {
    MOCK_SITE_WITH_LOCATION_IN_LO,
    MOCK_SITE_WITH_LOCATION_IN_MO,
    MOCK_SITE_WITH_LOCATION_IN_OTHER_REGION,
    MOCK_SITE_WITH_LOCATION_WITHOUT_POPULATED_RGID,
} from './mocks';

describe('getSitePopulatedRgid', () => {
    it('отдает subjectFederationRgid если он является ргидом М+МО или С+ЛО', () => {
        expect(getSitePopulatedRgid(MOCK_SITE_WITH_LOCATION_IN_MO)).toBe(741964);
        expect(getSitePopulatedRgid(MOCK_SITE_WITH_LOCATION_IN_LO)).toBe(741965);
    });

    it('отдает populatedRgid если subjectFederationRgid не является ргидом М+МО или С+ЛО', () => {
        expect(getSitePopulatedRgid(MOCK_SITE_WITH_LOCATION_IN_OTHER_REGION)).toBe(333444);
    });

    it('отдает rgid если subjectFederationRgid не является ргидом М+МО или С+ЛО, а populatedRgid отсутствует', () => {
        expect(getSitePopulatedRgid(MOCK_SITE_WITH_LOCATION_WITHOUT_POPULATED_RGID)).toBe(222333);
    });
});
