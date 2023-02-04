import { IPaymentStore } from 'realty-core/view/react/deskpad/reducers/payment';
import { AddressInfoStatus, PaidReportStatus, PaymentStatus } from 'realty-core/view/react/common/types/egrnPaidReport';
import { StageName } from 'realty-core/view/react/deskpad/reducers/payment/types';

export const timeoutStore: { payment: IPaymentStore<'juridicalEGRNPaidReport'> } = {
    payment: {
        juridicalEGRNPaidReport: {
            popup: {
                isOpened: true,
                data: {
                    offerId: '8207175651726858496',
                },
            },
            stages: {
                init: {
                    isLoaded: true,
                    price: {
                        isAvailable: true,
                        effective: 123,
                        base: 410,
                        reasons: [],
                        modifiers: {
                            bonusDiscount: {
                                percent: 70,
                            },
                        },
                    },
                },
                perform: {
                    isLoaded: true,
                },
                status: {
                    status: PaymentStatus.READY_TO_PAY,
                    isLoaded: false,
                },
            },
            currentStageName: StageName.status,
            isLoading: false,
            hasError: false,
            addressInfo: {
                addressInfoId: '875201527daa4db18aebc3d94a68af22',
                userObjectInfo: {
                    offerId: '8207175651726858496',
                },
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Москва, Чертановская улица, 48к2',
                    floor: '10',
                    area: 53.4,
                    subjectFederationId: 1,
                },
                status: AddressInfoStatus.DONE,
                price: {
                    isAvailable: true,
                    effective: 123,
                    base: 410,
                    reasons: [],
                    modifiers: {
                        bonusDiscount: {
                            percent: 70,
                        },
                    },
                },
            },
            paidReport: {
                paidReportId: '9b6c54d0dd4648f5b51951b1f033a5a3',
                addressInfoId: '875201527daa4db18aebc3d94a68af22',
                address: {
                    addressInfoId: '875201527daa4db18aebc3d94a68af22',
                    userObjectInfo: {
                        offerId: '8207175651726858496',
                    },
                    evaluatedObjectInfo: {
                        unifiedAddress: 'Россия, Москва, Чертановская улица, 48к2',
                        floor: '10',
                        area: 53.4,
                        subjectFederationId: 1,
                    },
                    status: AddressInfoStatus.DONE,
                },
                paymentStatus: PaymentStatus.READY_TO_PAY,
                reportStatus: PaidReportStatus.NEW,
                reportDate: '2020-11-02T08:55:06.502Z',
                uid: '4056746537',
            },
            balance: {
                clientId: 1340247147,
                orderId: 143312,
                totalIncome: 50000000,
                totalSpent: 4924,
                hold: 0,
                current: 49995076,
            },
        },
    },
};
