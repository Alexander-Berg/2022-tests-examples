import { SiteInfoWithLocation } from '../index';

export const MOCK_SITE_WITH_LOCATION_IN_MO: SiteInfoWithLocation = {
    location: {
        rgid: 222333,
        populatedRgid: 333444,
        subjectFederationRgid: 741964,
    },
};

export const MOCK_SITE_WITH_LOCATION_IN_LO: SiteInfoWithLocation = {
    location: {
        rgid: 222333,
        populatedRgid: 333444,
        subjectFederationRgid: 741965,
    },
};

export const MOCK_SITE_WITH_LOCATION_IN_OTHER_REGION: SiteInfoWithLocation = {
    location: {
        rgid: 222333,
        populatedRgid: 333444,
        subjectFederationRgid: 444555,
    },
};

export const MOCK_SITE_WITH_LOCATION_WITHOUT_POPULATED_RGID: SiteInfoWithLocation = {
    location: {
        rgid: 222333,
        subjectFederationRgid: 444555,
    },
};
