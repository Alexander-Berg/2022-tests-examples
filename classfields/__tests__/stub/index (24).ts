import { getCardInfoByPan } from '@realty-front/payment-cards/app/libs';

import { PaymentCardId, IPaymentCard } from 'types/paymentCard';

export const tinkoffCard: IPaymentCard = {
    cardId: '1234' as PaymentCardId,
    pan: '4377720000000777',
    expDate: '11/22',
    info: getCardInfoByPan('4377720000000777'),
};

export const sberbankCard: IPaymentCard = {
    cardId: '123456' as PaymentCardId,
    pan: '5154975555555599',
    expDate: '11/22',
    info: getCardInfoByPan('5154975555555599'),
};
