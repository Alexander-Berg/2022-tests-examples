import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { ICommonPageStore } from 'view/react/deskpad/reducers/roots/common';

import { transactionWithOffer } from '../../../__tests__/stub/transactions';

export const store: DeepPartial<ICommonPageStore> = {
    wallet: {
        transactions: {
            page: {
                number: 0,
                size: 5,
            },
            total: 7548,
            values: [
                { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '1' } },
                { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '2' } },
                { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '3' } },
                { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '4' } },
                { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '5' } },
            ],
        },
        transactionsStatus: RequestStatus.LOADED,
        offers: {
            '8303246355285259777': {
                offerType: 'SELL',
                offerCategory: 'APARTMENT',
                apartment: {
                    rooms: 2,
                    studio: false,
                    openPlan: false,
                },
                price: {
                    value: '3400000',
                    currency: 'RUB',
                },
                area: {
                    value: 62.2,
                    unit: 'SQ_M',
                },
                location: {
                    localityName: 'Москва',
                    closestMetroName: 'Пражская',
                    rgid: '193336',
                },
            },
        },
    },
};
