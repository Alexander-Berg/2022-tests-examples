jest.mock('auto-core/lib/core/isMobileApp');

const preparer = require('./vasPromo');
const { promoServices } = require('auto-core/lib/util/vas/dicts');
const getServiceDiscountResponseMock = require('auto-core/server/resources/publicApiBilling/methods/getServiceDiscount.nock.fixture');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const isMobileApp = require('auto-core/lib/core/isMobileApp');

let response;

beforeEach(() => {
    response = {
        getServicesDiscount: getServiceDiscountResponseMock.with_active_discount(),
        bunker: getBunkerMock([ 'common/vas', 'common/vas_vip' ], { spreadSubNodes: true }),
    };
});

describe('не показывает попап, если', () => {
    it('у пользователя нет ни одного активного объявления', () => {
        response.getServicesDiscount.offer = undefined;
        const result = preparer(response);

        expect(result).toBeNull();
    });

    it('нет скидки', () => {
        response.getServicesDiscount.available_discount.discount = undefined;
        const result = preparer(response);

        expect(result).toBeNull();
    });

    it('у скидки нет срока', () => {
        response.getServicesDiscount.available_discount.to = undefined;
        const result = preparer(response);

        expect(result).toBeNull();
    });

    it('у пользователя есть вип пакет и только одна активная объява', () => {
        response.getServicesDiscount.offer = cloneOfferWithHelpers(cardMock)
            .withActiveVas([ 'package_vip' ])
            .value();
        response.getServicesDiscount.more_than_one_offer = false;
        const result = preparer(response);

        expect(result).toBeNull();
    });

    it('все сервисы со скидкой уже были куплены пользователем и у него только одна активная объява', () => {
        response.getServicesDiscount.more_than_one_offer = false;
        response.getServicesDiscount.available_discount.services = [ 'package_turbo', 'package_express' ].map((service) => ({ service }));
        response.getServicesDiscount.offer = cloneOfferWithHelpers(cardMock)
            .withActiveVas([ 'package_turbo', 'package_express' ])
            .value();
        const result = preparer(response);

        expect(result).toBeNull();
    });

    it('сервисов со скидкой нет', () => {
        response.getServicesDiscount.available_discount.services = [];
        const result = preparer(response);

        expect(result).toBeNull();
    });
});

it('отдаст корректный ответ', () => {
    const result = preparer(response);

    expect(result.offer).toBe(response.getServicesDiscount.offer);

    // не хотим снимать offer
    delete result.offer;
    expect(result).toMatchSnapshot();
});

describe('для пользователя с одним активным объявлением массив предлагаемых сервисов', () => {
    beforeEach(() => {
        response.getServicesDiscount.more_than_one_offer = false;
        response.getServicesDiscount.available_discount.services =
            [ 'package_vip', 'package_turbo', 'package_express', 'all_sale_toplist', 'all_sale_special' ]
                .map((service) => ({ service }));
    });

    it('будет иметь три элемента, если у пользователя 3 и больше неактивных сервиса', () => {
        const result = preparer(response).services;

        expect(result).toHaveLength(3);
    });

    it('будет иметь два элемента, если у пользователя 2 неактивных сервиса', () => {
        response.getServicesDiscount.offer = cloneOfferWithHelpers(cardMock)
            .withPrice(response.bunker.minvalue - 10000)
            .withCustomVas({ service: 'package_vip', recommendation_priority: 0 })
            .withActiveVas([ 'package_turbo', 'package_express' ])
            .value();
        const result = preparer(response).services;

        expect(result).toHaveLength(2);
    });

    it('будет иметь один элемент, если у пользователя 1 неактивный сервис', () => {
        response.getServicesDiscount.offer = cloneOfferWithHelpers(cardMock)
            .withPrice(response.bunker.minvalue - 10000)
            .withCustomVas({ service: 'package_vip', recommendation_priority: 0 })
            .withActiveVas([ 'package_turbo', 'package_express', 'all_sale_toplist' ])
            .value();
        const result = preparer(response).services;

        expect(result).toHaveLength(1);
    });

    it('будет отсортирован в заданном порядке', () => {
        const result = preparer(response).services.map(mapToServiceId);
        const master = promoServices.filter((item) => result.includes(item));

        expect(result).toEqual(master);
    });

    it('не будет включать вип-пакет, если цена объявления ниже порога', () => {
        response.getServicesDiscount.offer = cloneOfferWithHelpers(cardMock)
            .withPrice(response.bunker.minvalue - 10000)
            .withCustomVas({ service: 'package_vip', recommendation_priority: 0 })
            .value();
        const result = preparer(response).services.map(mapToServiceId);

        expect(result).not.toContain('package_vip');
    });

    it('будет включать вип-пакет, если цена объявления выше порога', () => {
        response.getServicesDiscount.offer = cloneOfferWithHelpers(cardMock)
            .withPrice(response.bunker.minvalue + 10000)
            .withCustomVas({ service: 'package_vip', recommendation_priority: 0 })
            .withActiveVas([])
            .value();
        const result = preparer(response).services.map(mapToServiceId);

        expect(result).toContain('package_vip');
    });

    it('будет включать срок действия услуг из ответа бэка а не из бункера', () => {
        const { days: resultDays, service } = preparer(response).services[0];
        const backEndDays = response.getServicesDiscount.offer.service_prices.find((s) => s.service === service).days;
        const bunkerDays = response.bunker[service].days;

        expect(resultDays).toBe(backEndDays);
        expect(resultDays).not.toBe(bunkerDays);
    });
});

describe('для пользователя с 2-мя и более активными объявлениями массив предлагаемых сервисов', () => {
    beforeEach(() => {
        response.getServicesDiscount.more_than_one_offer = true;
        response.getServicesDiscount.available_discount.services =
            [ 'package_vip', 'package_turbo', 'all_sale_toplist' ]
                .map((service) => ({ service }));
    });

    it('будет содержать услуги вне зависимости от того подключены они для первой активной объявы или нет', () => {
        response.getServicesDiscount.offer = cloneOfferWithHelpers(cardMock)
            .withPrice(response.bunker.minvalue + 10000)
            .withActiveVas([ 'package_turbo', 'all_sale_toplist' ])
            .value();

        const servicesWithDiscount = response.getServicesDiscount.available_discount.services.map(mapToServiceId).slice(0, 3);
        const result = preparer(response).services.map(mapToServiceId);

        expect(result).toEqual(servicesWithDiscount);
    });

    it('будет содержать услуги даже если для первой активной объявы был куплен вип', () => {
        response.getServicesDiscount.offer = cloneOfferWithHelpers(cardMock)
            .withPrice(response.bunker.minvalue + 10000)
            .withActiveVas([ 'package_vip' ])
            .value();
        const result = preparer(response);

        expect(result.services).toHaveLength(3);
    });

    it('будет включать вип даже если для первой активной объявы он не доступен', () => {
        response.getServicesDiscount.offer = cloneOfferWithHelpers(cardMock)
            .withPrice(response.bunker.minvalue - 10000)
            .value();
        const result = preparer(response).services.map(mapToServiceId);

        expect(result).toContain('package_vip');
    });
});

it('в мобилке результат будет включать все сервисы, а не максимум 3', () => {
    isMobileApp.mockReturnValue(true);
    response.getServicesDiscount.more_than_one_offer = false;
    response.getServicesDiscount.available_discount.services =
        [ 'package_vip', 'package_turbo', 'package_express', 'all_sale_toplist', 'all_sale_special' ]
            .map((service) => ({ service }));

    const result = preparer(response).services;

    expect(result).toHaveLength(5);
});

function mapToServiceId({ service }) {
    return service;
}
