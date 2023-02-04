import { DeepPartial } from 'utility-types';

import dayjs from '@realty-front/dayjs';

import { RequestStatus } from 'realty-core/types/network';

import { FlatId, IManagerFlatPayment } from 'types/flat';
import { ContractId } from 'types/contract';
import { PaymentId, PaymentStatus } from 'types/payment';

import { IUniversalStore } from 'view/modules/types';
import { initialState as network } from 'view/modules/managerFlatPaymentStatusForm/reducers/network';
import { Fields } from 'view/modules/managerFlatPaymentStatusForm/types';

const SERVER_TIME_STAMP = 1636702668061;

export const payment: DeepPartial<IManagerFlatPayment> = {
    id: '38fc0645096c6f7e8bf9bcc8c1bab524' as PaymentId,
    status: PaymentStatus.FUTURE_PAYMENT,
    tenantPaymentDate: '2021-12-11T13:21:49.490Z',
    contract: {
        contractId: '12d5c10662dd40d4a96fee6eec6b819b' as ContractId,
        rentStartDate: '2021-11-10T21:00:00Z',
        contractNumber: '55',
        ownerPerson: {
            name: 'Олег',
            surname: 'Владелец',
            patronymic: 'Владелевич',
        },
        tenantPerson: {
            name: 'Олег',
            surname: 'Жилец',
            patronymic: 'Жилецович',
        },
    },
    tenantRentPayment: {
        startTime: '2021-12-10T21:00:00Z',
        endTime: '2021-12-09T21:00:00Z',
        contractPaymentDate: '2021-11-10T21:00:00Z',
        startDate: '2021-12-11',
        endDate: '2021-12-10',
        paymentDate: '2021-12-11',
        daysToPayment: 0,
        overdueDays: 0,
    },
    moderationSpecificPaymentInfo: {
        tenantAmount: '11000',
        ownerAmount: '10000',
        tenantPaymentDetails: {
            cardPayment: {
                panMask: '500000******0009',
            },
        },
    },
};

export const storeWithSkeleton: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const getStore = (status: PaymentStatus, isMobile?: string): DeepPartial<IUniversalStore> => {
    return {
        managerFlat: {
            flat: {
                flatId: '0000000000000' as FlatId,
            },
        },
        managerFlatPayment: {
            paymentWithContract: {
                payment: {
                    ...payment,
                    status,
                },
            },
        },
        managerFlatPaymentStatusForm: {
            fields: {
                [Fields.CURRENT_STATUS]: {
                    id: Fields.CURRENT_STATUS,
                    value: status,
                },
                [Fields.NEW_STATUS]: {
                    id: Fields.NEW_STATUS,
                    value: '',
                },
                [Fields.PAYMENT_DATE]: {
                    id: Fields.PAYMENT_DATE,
                    value: dayjs(SERVER_TIME_STAMP).format('DD.MM.YYYY'),
                },
            },
            network,
        },
        spa: {
            status: RequestStatus.LOADED,
        },
        config: {
            serverTimeStamp: SERVER_TIME_STAMP,
            isMobile,
        },
    };
};
