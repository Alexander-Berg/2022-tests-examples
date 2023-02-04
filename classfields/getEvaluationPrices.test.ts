import getEvaluationPrices from './getEvaluationPrices';

const discounts = [
    {
        region_id: 1,
        section: 'NEW',
        mark: 'AUDI',
        model: 'A4',
        tradein_discount_median: 100000,
    },
    {
        region_id: 1,
        section: 'USED',
        mark: 'AUDI',
        model: 'A4',
        tradein_discount_median: 200000,
    },
    {
        region_id: 1,
        section: 'USED',
        mark: 'BMW',
        model: 'X5',
        tradein_discount_median: 150000,
    },
    {
        region_id: 1,
        section: 'USED',
        mark: 'CSS',
        model: 'X5',
        tradein_discount_median: 200000,
    },
];

const prices = {
    autoru: {
        from: 770000,
        to: 840000,
        currency: 'RUR',
    },
    tradein: {
        from: 668000,
        to: 738000,
        currency: 'RUR',
    },
    market: {
        currency: 'RUR',
        price: 805000,
    },
    tradein_dealer_matrix_new: {
        from: 649000,
        to: 719000,
        currency: 'RUR',
    },
    tradein_dealer_matrix_used: {
        from: 665000,
        to: 735000,
        currency: 'RUR',
    },
    tradein_dealer_matrix_buyout: {
        from: 713000,
        to: 784000,
        currency: 'RUR',
    },
};

describe('в экспе', () => {
    const hasTradeinExp = true;
    it('должен правильно вернуть цены для оценки авто без учета скидок трейдина', () => {
        const result = getEvaluationPrices({ prices, discounts, hasTradeinExp });
        expect(result).toEqual({
            autoru: [ { price: 805000 } ],
            tradein: [ {
                price: {
                    currency: 'RUR',
                    from: 713000,
                    to: 784000,
                },
            } ] });
    });

    it('должен правильно вернуть цены для оценки авто со скидками', () => {
        const result = getEvaluationPrices({
            prices,
            discounts,
            reason: 'tradein-new',
            markModels: [ { mark: 'AUDI', models: [ { id: 'A4', generations: [], nameplates: [] } ] } ],
            geo: 222,
            geoParentsIds: [ 1, 2, 3 ],
            hasTradeinExp,
        });
        expect(result).toEqual({
            autoru: [ { price: 805000 } ],
            tradein: [ {
                discount: 100000,
                mark: 'AUDI',
                model: 'A4',
                price: {
                    from: 749000,
                    to: 819000,
                    currency: 'RUR',
                },
            } ],
        });
    });

    it('должен правильно вернуть цены для оценки авто со скидками для нескольких марко-моделей', () => {
        const result = getEvaluationPrices({
            prices,
            discounts,
            reason: 'tradein-used',
            markModels: [
                { mark: 'AUDI', models: [ { id: 'A4', generations: [], nameplates: [] } ] },
                { mark: 'BMW', models: [ { id: 'X5', generations: [], nameplates: [] } ] },
            ],
            geo: 1,
            geoParentsIds: [],
            hasTradeinExp,
        });
        expect(result).toEqual({
            autoru: [ { price: 805000 } ],
            tradein: [
                {
                    discount: 200000,
                    mark: 'AUDI',
                    model: 'A4',
                    price: {
                        from: 865000,
                        to: 935000,
                        currency: 'RUR',
                    },
                },
                {
                    discount: 150000,
                    mark: 'BMW',
                    model: 'X5',
                    price: {
                        from: 815000,
                        to: 885000,
                        currency: 'RUR',
                    },
                },
            ],
        });
    });

    it('должен правильно вернуть цены для оценки авто, если вдруг нет списка скидок', () => {
        const result = getEvaluationPrices({
            prices,
            discounts: undefined,
            reason: 'tradein-new',
            markModels: [ { mark: 'AUDI', models: [ { id: 'A4', generations: [], nameplates: [] } ] } ],
            geo: 222,
            geoParentsIds: [ 1, 2, 3 ],
            hasTradeinExp,
        });
        expect(result).toEqual({
            autoru: [ { price: 805000 } ],
            tradein: [ {
                discount: 0,
                mark: 'AUDI',
                model: 'A4',
                price: {
                    currency: 'RUR',
                    from: 649000,
                    to: 719000,
                },
            } ],
        });
    });
});

describe('без экспа', () => {
    const hasTradeinExp = false;
    it('должен правильно вернуть цены для оценки авто без учета скидок трейдина', () => {
        const result = getEvaluationPrices({ prices, discounts, hasTradeinExp });
        expect(result).toEqual({
            autoru: [ { price: 805000 } ],
            tradein: [ { price: { currency: 'RUR', from: 713000, to: 784000 } } ],
        });
    });

    it('должен правильно вернуть цены для оценки авто со скидками', () => {
        const result = getEvaluationPrices({
            prices,
            discounts,
            reason: 'tradein-new',
            markModels: [ { mark: 'AUDI', models: [ { id: 'A4', generations: [], nameplates: [] } ] } ],
            geo: 222,
            geoParentsIds: [ 1, 2, 3 ],
            hasTradeinExp,
        });
        expect(result).toEqual({
            autoru: [ { price: 805000 } ],
            tradein: [ { price: { currency: 'RUR', from: 713000, to: 784000 } } ],
        });
    });

    it('должен правильно вернуть цены для оценки авто со скидками для нескольких марко-моделей', () => {
        const result = getEvaluationPrices({
            prices,
            discounts,
            reason: 'tradein-used',
            markModels: [
                { mark: 'AUDI', models: [ { id: 'A4', generations: [], nameplates: [] } ] },
                { mark: 'BMW', models: [ { id: 'X5', generations: [], nameplates: [] } ] },
            ],
            geo: 1,
            geoParentsIds: [],
            hasTradeinExp,
        });
        expect(result).toEqual({
            autoru: [ { price: 805000 } ],
            tradein: [ { price: { currency: 'RUR', from: 713000, to: 784000 } } ],
        });
    });

    it('должен правильно вернуть цены для оценки авто, если вдруг нет списка скидок', () => {
        const result = getEvaluationPrices({
            prices,
            discounts: undefined,
            reason: 'tradein-new',
            markModels: [ { mark: 'AUDI', models: [ { id: 'A4', generations: [], nameplates: [] } ] } ],
            geo: 222,
            geoParentsIds: [ 1, 2, 3 ],
            hasTradeinExp,
        });
        expect(result).toEqual({
            autoru: [ { price: 805000 } ],
            tradein: [ { price: { currency: 'RUR', from: 713000, to: 784000 } } ],
        });
    });
});
