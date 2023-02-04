import { DeepPartial } from 'utility-types';

import { ProfileTypes } from 'realty-core/types/profile/profileTypes';
import { OfferTypes, OfferCategories } from 'realty-core/types/searcher/filters';
import { IGeoSuggest } from 'realty-core/types/profile/geoSuggest';
import { RequestStatus } from 'realty-core/types/network';

import { IPageData, IProfileSearchQuery } from 'realty-core/view/react/modules/profiles/redux/types';

import { IProfilesPageStore } from 'view/react/deskpad/reducers/roots/profiles';

export const getStore = ({
    profiles,
    searchQuery,
    geoSuggest,
    locative,
    page,
    searchCount,
}: {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    profiles: { order: Array<string>; ids: any };
    searchQuery?: Partial<IProfileSearchQuery>;
    geoSuggest?: IGeoSuggest;
    locative?: string;
    page?: IPageData;
    searchCount?: number;
}): DeepPartial<IProfilesPageStore> => ({
    profiles: {
        redirectPhones: {},
        profileSearch: {
            ...profiles,
            page: page || { current: 1, total: 1 },
            searchQuery: {
                rgid: 587795,
                userType: ProfileTypes.AGENCY,
                type: OfferTypes.SELL,
                category: OfferCategories.APARTMENT,
                ...searchQuery,
            },
            geoSuggest: geoSuggest || {
                label: 'Москва и МО',
                rgid: 587795,
            },
            searchCount: searchCount !== undefined ? searchCount : profiles.order.length,
            status: RequestStatus.INITIAL,
            locative: locative !== undefined ? locative : 'в Москве и МО',
        },
    },
});
