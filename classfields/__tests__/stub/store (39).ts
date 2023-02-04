import { DeepPartial } from 'utility-types';

import { PaymentSystemId } from 'realty-core/types/payment/purchase';

import { ICommonPageStore } from 'view/react/deskpad/reducers/roots/common';

export const oneCardStore: DeepPartial<ICommonPageStore> = {
    wallet: {
        cards: [
            {
                psId: PaymentSystemId.YANDEXKASSA_V3,
                id: 'bank_card',
                type: 'visa',
                mask: '**** **** **** 1111',
                cddPanMask: '411111|1111',
            },
        ],
    },
};

export const severalCardsStore: DeepPartial<ICommonPageStore> = {
    wallet: {
        cards: [
            {
                psId: PaymentSystemId.YANDEXKASSA_V3,
                id: 'bank_card',
                type: 'visa',
                mask: '**** **** **** 1111',
                cddPanMask: '411111|1111',
            },
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
                type: 'maestro',
                mask: '**** **** **** 4411',
                cddPanMask: '555555|4411',
            },
            {
                psId: PaymentSystemId.YANDEXKASSA_V3,
                id: 'bank_card',
                type: 'mir',
                mask: '**** **** **** 1144',
                cddPanMask: '555555|1144',
            },
            {
                psId: PaymentSystemId.YANDEXKASSA_V3,
                id: 'bank_card',
                type: 'jcb',
                mask: '**** **** **** 6666',
                cddPanMask: '555555|6666',
            },
        ],
    },
};
