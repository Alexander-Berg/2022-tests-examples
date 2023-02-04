import {
    IPaymentMethod,
    PaymentMethodType,
    PaymentSystemId,
    IPaymentMethodTiedCard,
    CardBrand,
    IPaymentMethodWallet,
} from 'realty-core/types/payment/purchase';

export const bankCardPaymentMethod: IPaymentMethod = {
    id: 'bank_card',
    name: 'Банковская карта',
    needEmail: true,
    psId: PaymentSystemId.YANDEXKASSA_V3,
    type: PaymentMethodType.bankCard,
};

export const sberbankPaymentMethod: IPaymentMethod = {
    id: 'sberbank',
    name: 'Сбербанк Онлайн',
    needEmail: true,
    psId: PaymentSystemId.YANDEXKASSA_V3,
    type: PaymentMethodType.sberbank,
};

export const yooMoneyPaymentMethod: IPaymentMethod = {
    id: 'yoo_money',
    name: 'ЮMoney',
    needEmail: true,
    psId: PaymentSystemId.YANDEXKASSA_V3,
    type: PaymentMethodType.yooMoney,
};

export const walletPaymentMethod: IPaymentMethodWallet = {
    balance: '44800',
    needEmail: false,
    type: PaymentMethodType.wallet,
};

export const getTiedCard = (brand: CardBrand): IPaymentMethodTiedCard => ({
    id: 'bank_card',
    name: 'Банковская карта',
    needEmail: true,
    properties: {
        card: {
            cddPanMask: '555555|4444',
            brand,
            expireYear: '2024',
            expireMonth: '1',
        },
    },
    psId: PaymentSystemId.YANDEXKASSA_V3,
    type: PaymentMethodType.tiedCard,
});
