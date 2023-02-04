import * as decl from 'realty-core/app/lib/filters/decl/secondary';

export const getInitialState = ({
    extraShown = false,
    forms = {},
    refinements = [
        'metro',
        'directions',
        'sub-localities',
        'map-area'
    ]
} = {}) => ({
    filters: {
        offers: {
            matchedQuantity: 989,
            sections: {
                mainShown: true,
                extraShown
            },
            decl
        }
    },
    geo: {
        rgid: 741964,
        isInMO: true,
        hasYandexRent: true,
        refinements,
        parents: []
    },
    forms: {
        offers: forms
    },
    page: {
        isLoading: false
    },
    ads: {
        blocks: {}
    }
});
