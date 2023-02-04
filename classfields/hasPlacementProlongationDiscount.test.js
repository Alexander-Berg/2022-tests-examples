const MockDate = require('mockdate');

const hasPlacementProlongationDiscount = require('./hasPlacementProlongationDiscount');

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

beforeEach(() => {
    MockDate.set('2020-03-18T13:00:00Z');
});

afterEach(() => {
    MockDate.reset();
});

it('вернет true, если время не вышло, оффер не активен и пользователь - перекуп', () => {
    const offer = cloneOfferWithHelpers(cardMock)
        .withStatus('INACTIVE')
        .withExpireDate('2020-03-18T12:00:00Z')
        .withCustomVas({
            service: 'all_sale_activate',
            price: 999,
            original_price: 1777,
            days: 7,
            prolongation_forced_not_togglable: true,
            prolongation_interval_will_expire: '2020-03-18T13:55:33Z',
        })
        .value();

    const result = hasPlacementProlongationDiscount(offer);

    expect(result).toBe(true);
});

describe('вернет false если', () => {
    it('пользователь не перекуп', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withStatus('INACTIVE')
            .withExpireDate('2020-03-18T12:00:00Z')
            .withCustomVas({
                service: 'all_sale_activate',
                price: 999,
                original_price: 1777,
                days: 7,
                prolongation_forced_not_togglable: false,
                prolongation_interval_will_expire: '2020-03-18T13:55:33Z',
            })
            .value();

        const result = hasPlacementProlongationDiscount(offer);

        expect(result).toBe(false);
    });

    it('время вышло', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withStatus('INACTIVE')
            .withExpireDate('2020-03-18T12:00:00Z')
            .withCustomVas({
                service: 'all_sale_activate',
                price: 999,
                original_price: 1777,
                days: 7,
                prolongation_forced_not_togglable: true,
                prolongation_interval_will_expire: '2020-03-18T12:55:33Z',
            })
            .value();

        const result = hasPlacementProlongationDiscount(offer);

        expect(result).toBe(false);
    });

    it('оффер активен', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withStatus('ACTIVE')
            .withExpireDate('2020-03-18T12:00:00Z')
            .withCustomVas({
                service: 'all_sale_activate',
                price: 999,
                original_price: 1777,
                days: 7,
                prolongation_forced_not_togglable: true,
                prolongation_interval_will_expire: '2020-03-18T13:55:33Z',
            })
            .value();

        const result = hasPlacementProlongationDiscount(offer);

        expect(result).toBe(false);
    });

    it('нет скидки на размещение', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withStatus('INACTIVE')
            .withExpireDate('2020-03-18T12:00:00Z')
            .withCustomVas({
                service: 'all_sale_activate',
                price: 999,
                original_price: 999,
                days: 7,
                prolongation_forced_not_togglable: true,
                prolongation_interval_will_expire: '2020-03-18T13:55:33Z',
            })
            .value();

        const result = hasPlacementProlongationDiscount(offer);

        expect(result).toBe(false);
    });
});
