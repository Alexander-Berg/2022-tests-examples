const baseState = {
    config: {},
};

export const state = {
    ...baseState,
    samoletCard: {
        supportedRgids: [741964, 741965],
        sites: {
            searchQuery: {},
        },
    },
};

export const filledState = {
    ...baseState,
    samoletCard: {
        supportedRgids: [741964, 741965],
        sites: {
            searchQuery: {
                roomsTotal: ['STUDIO', '3', 'PLUS_4'],
                priceMin: 1234567,
                priceMax: 9876543,
                areaMin: 49,
                areaMax: 298,
                deliveryDate: ['3_2022'],
                rgid: 741965,
            },
        },
    },
};
