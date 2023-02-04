import type de from 'descript';

import type { MagListingPage } from 'auto-core/react/dataDomain/mag/StateMag';
import listingPageMock from 'auto-core/react/dataDomain/mag/listingPage';

import redirectIfArchivedAndGenerationTag from './redirectIfArchivedAndGenerationTag';

let listingPage: MagListingPage;
const mockCancelCallback = jest.fn();
class MockCancel {
    cancel(reason: de.DescriptError) {
        return mockCancelCallback(reason);
    }
}

beforeEach(() => {
    listingPage = listingPageMock.value();
});

describe('ничего не делает', () => {
    it('тег в архиве, но нет поколения', () => {
        listingPage.isArchived = true;
        const result = redirectIfArchivedAndGenerationTag(new MockCancel, listingPage);

        expect(result).toBeUndefined();
        expect(mockCancelCallback).toHaveBeenCalledTimes(0);
    });

    it('есть поколение, но тег не в архиве', () => {
        listingPage.mmm = 'VOLKSWAGEN,GOLF_GTI,20270377';
        const result = redirectIfArchivedAndGenerationTag(new MockCancel, listingPage);

        expect(result).toBeUndefined();
        expect(mockCancelCallback).toHaveBeenCalledTimes(0);
    });
});

describe('редиректит, если тег в архиве и есть поколение', () => {
    beforeEach(() => {
        listingPage.isArchived = true;
        listingPage.mmm = 'VOLKSWAGEN,GOLF_GTI,20270377';
    });

    const redirectResult = {
        error: {
            code: 'MAG_CAR_GENERATION_TAG_TO_CAR_MODEL_TAG',
            id: 'REDIRECTED',
            location: 'https://mag.autoru_frontend.base_domain/tag/volkswagen-golfgti/',
            status_code: 301,
        },
    };

    it('веб', () => {
        redirectIfArchivedAndGenerationTag(new MockCancel, listingPage);

        expect(mockCancelCallback).toHaveBeenCalledWith(redirectResult);
    });

    it('амп', () => {
        redirectIfArchivedAndGenerationTag(new MockCancel, listingPage, true);

        redirectResult.error.location = 'https://mag.autoru_frontend.base_domain/amp/tag/volkswagen-golfgti/';
        expect(mockCancelCallback).toHaveBeenCalledWith(redirectResult);
    });
});
