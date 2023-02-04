import { IOfferPageStore } from 'view/react/deskpad/reducers/roots/offer';

export const withRentPledgeAndUtilitiesFeeNotIncluded = {
    transactionConditionsMap: { RENT_PLEDGE: true },
    utilitiesFee: 'NOT_INCLUDED',
};

export const withRentPledgeAndUtilitiesIncluded = {
    transactionConditionsMap: { RENT_PLEDGE: true },
    utilitiesFee: 'INCLUDED',
};

export const withPriceRentPledgeAndUtilitiesNotIncluded = {
    transactionConditionsMap: { RENT_PLEDGE: true },
    rentDeposit: 25320,
    utilitiesFee: 'NOT_INCLUDED',
};

export const withoutRentPledgeAndUtilitiesNotIncluded = {
    utilitiesFee: 'NOT_INCLUDED',
};

type TFeesMock =
    | typeof withRentPledgeAndUtilitiesFeeNotIncluded
    | typeof withRentPledgeAndUtilitiesIncluded
    | typeof withPriceRentPledgeAndUtilitiesNotIncluded
    | typeof withoutRentPledgeAndUtilitiesNotIncluded;

export const defaultState = (feesMock: TFeesMock = withRentPledgeAndUtilitiesFeeNotIncluded) =>
    (({
        offerCard: {
            card: {
                agentFee: 27,
                ...feesMock,
                minRentPeriod: 8,
                price: {
                    currency: 'RUR',
                    value: 32725,
                },
            },
        },
    } as unknown) as IOfferPageStore);

export const yaArendaState = ({
    offerCard: {
        card: {
            agentFee: 50,
            ...withRentPledgeAndUtilitiesFeeNotIncluded,
            minRentPeriod: 1,
            price: {
                currency: 'RUR',
                value: 32000,
            },
            yandexRent: true,
        },
    },
} as unknown) as IOfferPageStore;
