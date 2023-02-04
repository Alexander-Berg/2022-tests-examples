import * as decl from 'realty-core/app/lib/filters/decl/secondary';

export const getInitialState = ({
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
            matchedQuantity: 115383,
            sections: {
                mainShown: true,
                extraShown: false
            },
            decl
        }
    },
    geo: {
        rgid: 741964,
        isInLO: true,
        refinements
    },
    offersLinkPresets: {
        presetsNewbuildingsData: [ 1, 2 ]
    },
    forms: {
        offers: forms
    },
    page: {
        isLoading: false
    }
});
