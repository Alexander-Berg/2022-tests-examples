import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { PaymentStatus } from 'types/payment';

import { IUniversalStore } from 'view/modules/types';

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    paymentsHistory: {},
};

const contract = {
    rentStartDate: '2021-01-20T21:00:00Z',
    contractNumber: '34-55',
    ownerPerson: {
        name: 'Gomer',
        surname: 'Simpson',
        patronymic: 'Семенович',
    },
    tenantPerson: {
        name: 'Геннадий',
        surname: 'Сарафанов',
        patronymic: 'Семенович',
    },
};

export const tenantStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    paymentsHistory: {
        isTenantPayments: true,
        sectionalPayments: [
            {
                day: '19.03.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-03-19T22:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-03-20T21:00:00Z',
                            endTime: '2021-04-19T21:00:00Z',
                            contractPaymentDate: '2021-03-20T21:00:00Z',
                        },
                        tenantSpecificPaymentInfo: {
                            amount: '5886100',
                            tenantPaymentDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                ],
            },
            {
                day: '20.02.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-20T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-20T21:00:00Z',
                        },
                        tenantSpecificPaymentInfo: {
                            amount: '5886100',
                            tenantPaymentDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-20T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-20T21:00:00Z',
                        },
                        tenantSpecificPaymentInfo: {
                            amount: '5886100',
                            tenantPaymentDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-20T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-20T21:00:00Z',
                        },
                        tenantSpecificPaymentInfo: {
                            amount: '5886100',
                            tenantPaymentDetails: {
                                phonePayment: {
                                    panMask: '+7 (951) ***-**-72',
                                },
                            },
                        },
                    },
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-20T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-20T21:00:00Z',
                        },
                        tenantSpecificPaymentInfo: {
                            amount: '5886100',
                            tenantPaymentDetails: {
                                sbpPayment: {
                                    panMask: '+7 (951) ***-**-72',
                                },
                            },
                        },
                    },
                ],
            },
            {
                day: '18.01.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-01-18T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-01-20T21:00:00Z',
                            endTime: '2021-02-19T21:00:00Z',
                            contractPaymentDate: '2021-01-20T21:00:00Z',
                        },
                        tenantSpecificPaymentInfo: {
                            amount: '5886100',
                            tenantPaymentDetails: {
                                bankTransfer: {},
                            },
                        },
                    },
                ],
            },
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 2,
            pageCount: 1,
        },
    },
};

export const outdatedTenantStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    paymentsHistory: {
        isTenantPayments: true,
        sectionalPayments: [
            {
                day: '20.03.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-03-19T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-03-19T21:00:00Z',
                            endTime: '2021-04-19T21:00:00Z',
                            contractPaymentDate: '2021-03-20T21:00:00Z',
                        },
                        tenantSpecificPaymentInfo: {
                            amount: '5886100',
                            tenantPaymentDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                ],
            },
            {
                day: '24.02.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-24T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-19T21:00:00Z',
                        },
                        tenantSpecificPaymentInfo: {
                            amount: '5886100',
                            tenantPaymentDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-24T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-19T21:00:00Z',
                        },
                        tenantSpecificPaymentInfo: {
                            amount: '5886100',
                            tenantPaymentDetails: {
                                bankTransfer: {},
                            },
                        },
                    },
                ],
            },
            {
                day: '23.01.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-01-23T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-01-20T21:00:00Z',
                            endTime: '2021-02-19T21:00:00Z',
                            contractPaymentDate: '2021-01-19T21:00:00Z',
                        },
                        tenantSpecificPaymentInfo: {
                            amount: '5886100',
                            tenantPaymentDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                ],
            },
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 2,
            pageCount: 1,
        },
    },
};

export const ownerStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    paymentsHistory: {
        isTenantPayments: false,
        sectionalPayments: [
            {
                day: '20.03.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-03-20T21:00:00Z',
                        ownerPaymentDate: '2021-03-20T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-03-20T21:00:00Z',
                            endTime: '2021-04-19T21:00:00Z',
                            contractPaymentDate: '2021-03-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                ],
            },
            {
                day: '21.02.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-21T21:00:00Z',
                        ownerPaymentDate: '2021-02-21T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-21T21:00:00Z',
                        ownerPaymentDate: '2021-02-21T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                bankTransfer: {},
                            },
                        },
                    },
                ],
            },
            {
                day: '22.01.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-01-21T21:00:00Z',
                        ownerPaymentDate: '2021-01-21T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-01-20T21:00:00Z',
                            endTime: '2021-02-19T21:00:00Z',
                            contractPaymentDate: '2021-01-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                ],
            },
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 2,
            pageCount: 1,
        },
    },
};

export const ownerStoreWithPaging: DeepPartial<IUniversalStore> = {
    ...ownerStore,
    paymentsHistory: {
        isTenantPayments: false,
        sectionalPayments: [
            {
                day: '20.03.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-03-20T21:00:00Z',
                        ownerPaymentDate: '2021-03-20T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-03-20T21:00:00Z',
                            endTime: '2021-04-19T21:00:00Z',
                            contractPaymentDate: '2021-03-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                ],
            },
            {
                day: '21.02.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-21T21:00:00Z',
                        ownerPaymentDate: '2021-02-21T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-21T21:00:00Z',
                        ownerPaymentDate: '2021-02-21T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                bankTransfer: {},
                            },
                        },
                    },
                ],
            },
            {
                day: '22.01.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-01-21T21:00:00Z',
                        ownerPaymentDate: '2021-01-21T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-01-20T21:00:00Z',
                            endTime: '2021-02-19T21:00:00Z',
                            contractPaymentDate: '2021-01-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-01-21T21:00:00Z',
                        ownerPaymentDate: '2021-01-21T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-01-20T21:00:00Z',
                            endTime: '2021-02-19T21:00:00Z',
                            contractPaymentDate: '2021-01-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '3242100',
                            ownerPayoutDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                ],
            },
        ],
        paging: {
            page: {
                num: 1,
                size: 5,
            },
            total: 7,
            pageCount: 2,
        },
    },
};

export const outdatedOwnerStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    paymentsHistory: {
        isTenantPayments: false,
        sectionalPayments: [
            {
                day: '20.03.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-03-20T21:00:00Z',
                        ownerPaymentDate: '2021-03-20T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-03-20T21:00:00Z',
                            endTime: '2021-04-19T21:00:00Z',
                            contractPaymentDate: '2021-03-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                ],
            },
            {
                day: '24.02.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-24T21:00:00Z',
                        ownerPaymentDate: '2021-02-24T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-02-24T21:00:00Z',
                        ownerPaymentDate: '2021-02-24T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-02-20T21:00:00Z',
                            endTime: '2021-03-19T21:00:00Z',
                            contractPaymentDate: '2021-02-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                bankTransfer: {},
                            },
                        },
                    },
                ],
            },
            {
                day: '23.01.2021',
                payments: [
                    {
                        status: PaymentStatus.PAID_TO_OWNER,
                        tenantPaymentDate: '2021-01-23T21:00:00Z',
                        ownerPaymentDate: '2021-01-23T21:00:00Z',
                        contract,
                        tenantRentPayment: {
                            startTime: '2021-01-20T21:00:00Z',
                            endTime: '2021-02-19T21:00:00Z',
                            contractPaymentDate: '2021-01-20T21:00:00Z',
                        },
                        ownerSpecificPaymentInfo: {
                            amount: '5886100',
                            ownerPayoutDetails: {
                                cardPayment: {
                                    panMask: '430000******0777',
                                },
                            },
                        },
                    },
                ],
            },
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 2,
            pageCount: 1,
        },
    },
};
