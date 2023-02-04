import getDecl from 'realty-core/app/lib/filters/mobile/decl';

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
            type: 'offers',
            matchedQuantity: 0,
            sections: {
                mainShown: true,
                extraShown
            },
            data: forms,
            decl: getDecl()
        }
    },
    geo: {
        rgid: 741964,
        refinements,
        parents: []
    },
    searchHistory: {},
    page: {
        isLoading: false
    }
});
