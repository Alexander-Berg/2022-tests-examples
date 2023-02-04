import { DeepPartial } from 'utility-types';

import { CardBrand } from '@realty-front/payment-cards/view/libs';

import { Flavor } from 'realty-core/types/utils';
import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { IPaymentCard } from 'types/paymentCard';

export const baseStore: DeepPartial<IUniversalStore> = {
    managerUser: {
        user: {
            userId: 'user2324' as Flavor<string, 'UserId'>,
            phone: '+79042406470',
            person: {
                name: 'Имя',
                surname: 'Фамилия',
                patronymic: 'Отчество',
            },
            calculatedInfo: {
                hasOwnerRequests: true,
                paymentDataProvided: true,
                isTenant: false,
            },
            email: 'useremail@yandex.ru',
        },
    },
    managerUserPaymentCards: {
        cards: [],
    },
    page: {
        params: {
            userId: 'user0000',
        },
    },
};

export const storeWithSkeleton: DeepPartial<IUniversalStore> = {
    page: {
        isLoading: true,
    },
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const storeWithUserCards = (limit: number): DeepPartial<IUniversalStore> => {
    const cardItem: IPaymentCard = {
        cardId: 'id0000' as Flavor<string, 'PaymentCardId'>,
        pan: '555555******4477',
        expDate: '0323',
        info: {
            pan: '555555******4477',
            brand: CardBrand.MASTERCARD,
            bank: {
                name: '',
                backgroundColor: '#2E2E2E',
                textColor: 'rgba(255, 255, 255, 0.92)',
                alias: '',
            },
        },
    };
    const cards: IPaymentCard[] = [];
    for (let i = 0; i < limit; i++) cards.push(cardItem);
    return {
        ...baseStore,
        managerUserPaymentCards: {
            cards,
        },
    };
};
