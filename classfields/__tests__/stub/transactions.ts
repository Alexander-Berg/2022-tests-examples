import {
    IWalletTransaction,
    WalletTransactionGoodProduct,
    WalletTransactionTarget,
    WalletTransactionType,
} from 'realty-core/types/wallet';
import { PaymentSystemId } from 'realty-core/types/payment/purchase';

export const transactionWithOffer: IWalletTransaction = {
    id: {
        id: '316c987e094f1b525a01df7bef89cd18',
        psId: PaymentSystemId.YANDEXKASSA_V3,
        type: WalletTransactionType.INCOMING,
    },
    account: '4015472092_1537279471773',
    timestamp: '2021-02-26T09:34:09.248Z',
    income: '9700',
    withdraw: '9700',
    overdraft: '0',
    payload: {
        struct: {
            goods: [
                {
                    cost: 9700,
                    duration: 1440,
                    offerId: '8303246355285259777',
                    payload: {},
                    product: WalletTransactionGoodProduct.RAISING,
                    productId: '3708f820d6504130a8a41982ecafdde9',
                },
            ],
            paymentId: '4867dfdadd7344c992b987ab609669dd',
            productLabel: '«Поднятие» (Автопродление)',
            purchase: '4867dfdadd7344c992b987ab609669dd',
            renewal: true,
            user: {
                uid: '4015472092',
            },
        },
    },
    activity: 'ACTIVE',
    target: WalletTransactionTarget.PURCHASE,
    refund: '0',
    user: '4015472092',
};

export const transactionWithOffers: IWalletTransaction = {
    id: {
        id: '0bfb61e265be4b328a9340bf3f03b074',
        type: WalletTransactionType.WITHDRAW,
    },
    account: '4015472092_1537279471773',
    timestamp: '2021-02-10T11:59:05.738Z',
    income: '0',
    withdraw: '17100',
    overdraft: '0',
    payload: {
        struct: {
            goods: [
                {
                    cost: 0,
                    duration: 10080,
                    offerId: '6138869419610291713',
                    payload: {},
                    product: WalletTransactionGoodProduct.TURBO_SALE,
                    productId: '49493f6e43e943279953300f4f130fdd',
                },
                {
                    cost: 0,
                    duration: 10080,
                    offerId: '840315172685253633',
                    payload: {},
                    product: WalletTransactionGoodProduct.TURBO_SALE,
                    productId: 'dc510337bd224ca498711c36d4a3c043',
                },
            ],
            paymentId: '0bfb61e265be4b328a9340bf3f03b074',
            productLabel: '«Пакет Турбо»',
            purchase: '0bfb61e265be4b328a9340bf3f03b074',
            renewal: false,
            user: {
                uid: '4015472092',
            },
        },
    },
    activity: 'ACTIVE',
    target: WalletTransactionTarget.PURCHASE,
    refund: '0',
    user: '4015472092',
};

export const transactionWithEGRNReport: IWalletTransaction = {
    id: {
        id: '7d1e1fe3fab24e8f9132cab4dc01c399',
        psId: PaymentSystemId.YANDEXKASSA_V3,
        type: WalletTransactionType.INCOMING,
    },
    account: '4015472092_1537279471773',
    timestamp: '2021-02-08T11:58:12.015Z',
    income: '41000',
    withdraw: '41000',
    overdraft: '0',
    payload: {
        struct: {
            goods: [
                {
                    cost: 0,
                    duration: 0,
                    offerId: '2341192864613709825',
                    payload: {
                        paidReportInfo: {
                            offerId: '2341192864613709825',
                            paidReportId: '492e3a3d30904a3fb53f9d930476fccc',
                        },
                    },
                    product: WalletTransactionGoodProduct.PAID_REPORT,
                    productId: 'cc8258fd7da24221ae2d22fdcd7dc13f',
                },
            ],
            paymentId: '7d1e1fe3fab24e8f9132cab4dc01c399',
            productLabel: '«Отчёт о квартире»',
            purchase: '7d1e1fe3fab24e8f9132cab4dc01c399',
            renewal: false,
            user: {
                uid: '4015472092',
            },
        },
    },
    activity: 'ACTIVE',
    target: WalletTransactionTarget.PURCHASE,
    refund: '0',
    user: '4015472092',
};

export const transactionWithAddingMoneyToWallet: IWalletTransaction = {
    id: {
        id: '423f095474cca0a12b829b50ccc46f9a',
        psId: PaymentSystemId.YANDEXKASSA_V3,
        type: WalletTransactionType.INCOMING,
    },
    account: '4015472092_1537279471773',
    timestamp: '2021-02-08T12:32:54.063Z',
    income: '1000',
    withdraw: '0',
    overdraft: '0',
    payload: {},
    activity: 'ACTIVE',
    target: WalletTransactionTarget.WALLET,
    refund: '0',
    user: '4015472092',
};
