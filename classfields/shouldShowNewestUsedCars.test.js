const shouldShowNewestUsedCars = require('./shouldShowNewestUsedCars');

const initialState = {
    listing: {
        data: {
            search_parameters: {
                category: 'cars',
                section: 'new',
                catalog_filter: [ { mark: 'Tesla', model: 'S' } ],
            },
            pagination: {
                total_offers_count: 0,
            },
        },
    },
    listingLocatorCounters: {
        data: [],
        pending: false,
    },
};

it('должен вернуть true для выдачи новых легковых, если выбрана одна марка  и модель и нет офферов,', () => {
    const currentState = {
        ...initialState,
        listing: {
            ...initialState.listing,
            filteredOffersCount: 0,
        },
    };

    expect(shouldShowNewestUsedCars(currentState)).toEqual(true);
});

it('должен вернуть false, если есть офферы в листинге', () => {
    const currentState = {
        ...initialState,
        listing: {
            ...initialState.listing,
            data: {
                ...initialState.listing.data,
                pagination: {
                    total_offers_count: 1,
                },
            },
        },
    };

    expect(shouldShowNewestUsedCars(currentState)).toEqual(false);
});

it('должен вернуть false для любой другой выдачи, кроме выдачи новых,', () => {
    const currentState = {
        ...initialState,
        listing: {
            ...initialState.listing,
            data: {
                ...initialState.listing.data,
                search_parameters: {
                    ...initialState.listing.data.search_parameters,
                    section: 'all',
                },
            },
        },
    };

    expect(shouldShowNewestUsedCars(currentState)).toEqual(false);
});

it('должен вернуть false, если выбрано больше одной марки-модели', () => {
    const currentState = {
        ...initialState,
        listing: {
            ...initialState.listing,
            data: {
                ...initialState.listing.data,
                search_parameters: {
                    ...initialState.listing.data.search_parameters,
                    catalog_filter: [ { mark: 'Tesla' }, { mark: 'Audi' } ],
                },
            },
            filteredOffersCount: 0,
        },
    };

    expect(shouldShowNewestUsedCars(currentState)).toEqual(false);
});

it('должен вернуть false, если выбрана только марка', () => {
    const currentState = {
        ...initialState,
        listing: {
            ...initialState.listing,
            data: {
                ...initialState.listing.data,
                search_parameters: {
                    ...initialState.listing.data.search_parameters,
                    catalog_filter: [ { mark: 'Tesla' } ],
                },
            },
            filteredOffersCount: 0,
        },
    };

    expect(shouldShowNewestUsedCars(currentState)).toEqual(false);
});
