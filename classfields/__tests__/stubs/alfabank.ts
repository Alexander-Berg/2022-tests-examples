import { IAlfaBankMortgageStore } from 'realty-core/view/react/modules/alfa-bank-mortgage/redux/reducer';

export const alfaBankMortgageInitial = ({
    areParamsFailed: false,
    areParamsLoading: false,
} as unknown) as IAlfaBankMortgageStore;

export const alfaBankMortgageLoaded: IAlfaBankMortgageStore = {
    ...alfaBankMortgageInitial,
    params: {
        rateBaseSecondary: 0.0829,
        rateDiscountYandex: 0.004,
        rateDiscountYandexSecondary: 0.007,
        rateDiscountBank: 0.001,
        rateSupportMax: 0.0599,
        rateSupportMin: 0.0599,
        regionalParams: [
            { geoId: 1, sumTransit: 6000000, sumSupportMax: 12000000 },
            { geoId: 10174, sumTransit: 5000000, sumSupportMax: 12000000 },
            { geoId: 225, sumTransit: 2500000, sumSupportMax: 6000000 },
        ],
        sumMin: 600000,
        sumMax: 20000000,
        costMin: 670000,
        costMax: 50000000,
        costDefault: 10000000,
        periodBaseMin: 3,
        periodBaseMax: 30,
        periodSupportMin: 2,
        periodSupportMax: 20,
        periodDefault: 20,
        downpaymentBaseNewMin: 0.1,
        downpaymentBaseSecondaryMin: 0.2,
        downpaymentBaseMax: 1,
        downpaymentBaseNewTransit: 0.2,
        downpaymentSumTransit: 0.2,
        downpaymentSupportMin: 0.15,
        downpaymentSupportMax: 1,
        downpaymentDefault: 0.3,
        rateBaseNewMin: 0.0829,
        rateBaseNewMax: 0.0899,
        sumMaxReduced: 20000000,
        brandInfo: { alfabank: { showStatic: true } },
        rateBaseHouse: 0.0979,
        downpaymentHouseMin: 0.5,
    },
};
