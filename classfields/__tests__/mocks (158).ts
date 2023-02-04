import decl from 'realty-core/app/lib/filters/decl/newbuilding';

export const getInitialState = ({
    extraShown = false,
    forms = {},
    refinements = ['metro', 'directions', 'sub-localities', 'map-area'],
    banks = [],
} = {}) => ({
    filters: {
        sites: {
            matchedQuantity: 815,
            sections: {
                mainShown: true,
                extraShown,
            },
            decl: decl(),
        },
    },
    geo: {
        id: 1,
        rgid: 741964,
        type: 'SUBJECT_FEDERATION',
        refinements,
    },
    forms: {
        sites: forms,
    },
    page: {
        isLoading: false,
    },
    search: {
        refinements: {},
        banks,
    },
});

export const getFilledInitialState = () =>
    getInitialState({
        forms: {
            roomsTotal: ['1', 'PLUS_4'],
            price: [10000000, null],
            'geo-refinement-multi': [
                {
                    label: 'Москва А101',
                    data: {
                        type: 'site',
                        params: {
                            siteId: 1,
                            siteName: 'Москва А101',
                        },
                    },
                },
            ],
            hasPark: true,
            hasPond: true,
            buildingClass: ['BUSINESS'],
        },
    });
