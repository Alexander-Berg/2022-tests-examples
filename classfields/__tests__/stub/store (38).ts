import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';
import { PaymentSystemId } from 'realty-core/types/payment/purchase';

import { ICommonPageStore } from 'view/react/deskpad/reducers/roots/common';
import { initialState } from 'view/react/deskpad/reducers/wallet';

import { transactionWithOffer } from './transactions';

export const notAuthorizedStore: DeepPartial<ICommonPageStore> = {
    user: {
        isAuth: false,
    },
    wallet: {
        isFilled: false,
    },
};

export const emptyStore: DeepPartial<ICommonPageStore> = {
    wallet: {
        ...initialState,
        isFilled: true,
    },
};

export const notEmptyStore: DeepPartial<ICommonPageStore> = {
    wallet: {
        isFilled: true,
        info: {
            balance: 2724,
            overdraft: 0,
            totalIncome: 128941019,
            totalRefund: 906000,
            totalSpent: 128938295,
        },
        preferWalletPayment: true,
        transactions: {
            page: {
                number: 0,
                size: 5,
            },
            total: 20,
            values: [
                { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '1' } },
                { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '2' } },
                { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '3' } },
                { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '4' } },
                { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '5' } },
            ],
        },
        transactionsStatus: RequestStatus.LOADED,
        bankerEmail: {
            email: 'wwwwww@yandex.ru',
        },
        bankerEmailStatus: RequestStatus.LOADED,
        cards: [
            {
                psId: PaymentSystemId.YANDEXKASSA_V3,
                id: 'bank_card',
                type: 'mastercard',
                mask: '**** **** **** 4444',
                cddPanMask: '555555|4444',
            },
            {
                psId: PaymentSystemId.YANDEXKASSA_V3,
                id: 'bank_card',
                type: 'jcb',
                mask: '**** **** **** 0000',
                cddPanMask: '352800|0000',
            },
        ],
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
