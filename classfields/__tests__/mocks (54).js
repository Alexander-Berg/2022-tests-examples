import decl from 'realty-core/app/lib/filters/decl/newbuilding';

export const getInitialState = ({
    extraShown = false,
    forms = {},
    refinements = [
        'metro',
        'directions',
        'sub-localities',
        'map-area'
    ],
    banks = []
} = {}) => ({
    filters: {
        sites: {
            matchedQuantity: 815,
            sections: {
                mainShown: true,
                extraShown
            },
            decl: decl()
        }
    },
    geo: {
        id: 1,
        rgid: 741964,
        type: 'SUBJECT_FEDERATION',
        refinements
    },
    forms: {
        sites: forms
    },
    page: {
        isLoading: false
    },
    search: {
        refinements: {},
        banks
    }
});
