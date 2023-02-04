const shouldShownSoldOffer = require('./shouldShownSoldOffer');
const LISTING_OUTPUT_TYPE = require('auto-core/data/listing/OutputTypes').default;

it('должен вернуть true, если категория - cars и section - не new', () => {
    const params = {
        category: 'cars',
        section: 'all',
    };
    const context = {
        req: {
            router: { params: {} },
            experimentsData: {
                has: () => {},
            },
            cookies: {},
        },
    };
    expect(shouldShownSoldOffer({ params, context })).toBe(true);
});

it('должен вернуть false, если категория - не cars', () => {
    const params = {
        category: 'moto',
        section: 'all',
    };
    const context = {
        req: {
            router: { params: {} },
            experimentsData: {
                has: () => {},
            },
            cookies: {},
        },
    };
    expect(shouldShownSoldOffer({ params, context })).toBe(false);
});

it('должен вернуть false, если секция - new', () => {
    const params = {
        category: 'cars',
        section: 'new',
    };
    const context = {
        req: {
            router: { params: {} },
            experimentsData: {
                has: () => {},
            },
            cookies: {},
        },
    };
    expect(shouldShownSoldOffer({ params, context })).toBe(false);
});

it('должен вернуть false, если сортировка отличается от дефолтной', () => {
    const params = {
        category: 'cars',
        section: 'all',
        sort: 'not-default',
    };
    const context = {
        req: {
            router: { params: {} },
            experimentsData: {
                has: () => {},
            },
            cookies: {},
        },
    };
    expect(shouldShownSoldOffer({ params, context })).toBe(false);
});

it('должен вернуть false, если outputType - не LIST', () => {
    const params = {
        category: 'cars',
        section: 'all',
        output_type: LISTING_OUTPUT_TYPE.MODELS,
    };
    const context = {
        req: {
            router: { params: {} },
            experimentsData: {
                has: () => {},
            },
            cookies: {},
        },
    };
    expect(shouldShownSoldOffer({ params, context })).toBe(false);
});
