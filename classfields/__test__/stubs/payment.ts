import { DeepPartial } from 'utility-types';

import merge from 'lodash/merge';

// eslint-disable-next-line max-len
import { CalculationStrategyNamespace_CalculationStrategy } from '@vertis/schema-registry/ts-types/realty/rent/api/rent_contract';
import { Commission_ServiceCommissionValue } from '@vertis/schema-registry/ts-types/realty/rent/api/commission';

import { Flavor } from 'realty-core/types/utils';

import { RequestStatus } from 'realty-core/types/network';
import { IUserPerson } from 'realty-core/types/yandex-arenda';

import { PaymentStatus, PayoutStatus, PayoutError, IPaymentDetails } from 'types/payment';
import { FlatUserRole, IContractOwnerInfo, IContractTenantInfo } from 'types/flat';
import { RentContractStatus } from 'types/contract';

import { IUniversalStore } from 'view/modules/types';

const getStateCheckPayment = (
    contractPaymentDate = '2021-03-15T21:00:00Z',
    rentStartDate = '2021-02-15T21:00:00Z',
    serverTimeStamp = '2021-03-16T21:00:00Z',
    paymentStatus: PaymentStatus = PaymentStatus.UNKNOWN,
    payoutStatus?: PayoutStatus,
    payoutError?: PayoutError,
    ownerPayoutDetails?: IPaymentDetails,
    tenantPaymentDetails?: IPaymentDetails
): DeepPartial<IUniversalStore> => {
    return merge({
        managerFlatPayment: {
            paymentWithContract: {
                payment: {
                    id: 'c17302963e49460cc5cb3daa606d4441' as Flavor<string, 'PaymentId'>,
                    status: paymentStatus,
                    contract: {
                        contractId: '97ab6be78cfe460fbf49ca1eff21bf45' as Flavor<string, 'ContractID'>,
                        rentStartDate: rentStartDate,
                        contractNumber: '3333',
                        ownerPerson: {
                            name: 'Антон',
                            surname: 'Антонов',
                            patronymic: 'Антонович',
                        },
                        tenantPerson: {
                            name: 'Семен',
                            surname: 'Семенов',
                            patronymic: 'Семенович',
                        },
                    },
                    tenantRentPayment: {
                        startTime: '2021-06-13T21:00:00Z',
                        endTime: '2021-07-12T21:00:00Z',
                        contractPaymentDate: contractPaymentDate,
                    },
                    moderationSpecificPaymentInfo: {
                        tenantAmount: '10000500',
                        ownerAmount: '9500000',
                        payoutStatus: payoutStatus,
                        payoutError: payoutError,
                        ownerPayoutDetails: ownerPayoutDetails,
                        tenantPaymentDetails: tenantPaymentDetails,
                        receipts: [],
                    },
                },
                contract: {
                    contractId: '97ab6be78cfe460fbf49ca1eff21bf45' as Flavor<string, 'ContractID'>,
                    rentStartDate: rentStartDate,
                    ownerInfo: {
                        person: {
                            name: 'Антон',
                            surname: 'Антонов',
                            patronymic: 'Антонович',
                        },
                        phone: '+79992134916',
                        inn: '380114872773',
                        email: '12312312@list.ru',
                        bankInfo: {
                            accountNumber: '12311231231231231223',
                            bic: '044444444',
                        },
                    },
                    tenantInfo: {
                        person: {
                            name: 'Семен',
                            surname: 'Семенов',
                            patronymic: 'Семенович',
                        },
                        phone: '+79992134916',
                        email: '12312312@list.ru',
                    },
                    rentAmount: '10000000',
                    paymentDayOfMonth: 14,
                    insurance: {
                        policyDate: '2020-12-10T21:00:00Z',
                        policyNumber: 'dsa123',
                        insuranceAmount: '500',
                    },
                    commissions: {
                        monthlyTenantCommission: '0',
                        monthlyOwnerCommission: '0.05',
                        oneTimeTenantCommission: '0',
                    },
                    calculatedAmounts: {
                        tenantPaymentAmount: '10000500',
                        ownerPaymentAmount: '10000000',
                        firstMonthPaymentAmount: '10000500',
                    },
                    housingAndCommunalServices: {
                        description: '',
                        meteringTransferDayOfMonth: 0,
                        comment: '12312312313',
                        paymentAgreement: '',
                    },
                    contractNumber: '3333',
                    houseInfo: {},
                    paymentsRegister: '231321321312',
                    status: RentContractStatus.ACTIVE,
                    calculationStrategy: 'STRATEGY_1',
                },
            },
        },
        managerFlat: {
            flat: {
                flatId: '597891f7f9314fd386c2850d6fb75d6e' as Flavor<string, 'FlatID'>,
            },
        },
        config: {
            serverTimeStamp: new Date(serverTimeStamp).getTime(),
        },
    });
};

const getStateCheckUser = (
    ownerPersonPayment?: Partial<IUserPerson> | undefined,
    tenantPersonPayment?: Partial<IUserPerson> | undefined,
    ownerPersonContract?: IContractOwnerInfo | undefined,
    tenantPersonContract?: IContractTenantInfo | undefined
): DeepPartial<IUniversalStore> => {
    return merge({
        managerFlatPayment: {
            paymentWithContract: {
                payment: {
                    id: 'c17302963e49460cc5cb3daa606d4441' as Flavor<string, 'PaymentId'>,
                    status: PaymentStatus.PAID_BY_TENANT,
                    tenantPaymentDate: '2021-07-12T21:00:00Z',
                    ownerPaymentDate: '2021-07-12T21:00:00Z',
                    contract: {
                        contractId: '97ab6be78cfe460fbf49ca1eff21bf45' as Flavor<string, 'ContractID'>,
                        rentStartDate: '2021-02-15T21:00:00Z',
                        contractNumber: '3333',
                        ownerPerson: ownerPersonPayment,
                        tenantPerson: tenantPersonPayment,
                    },
                    tenantRentPayment: {
                        startTime: '2021-06-13T21:00:00Z',
                        endTime: '2021-07-12T21:00:00Z',
                        contractPaymentDate: '2021-03-15T21:00:00Z',
                    },
                    moderationSpecificPaymentInfo: {
                        tenantAmount: '10000500',
                        ownerAmount: '9500000',
                        payoutStatus: PayoutStatus.PAID_OUT,
                        tenantPaymentDetails: {
                            cardPayment: {
                                panMask: '430000******0777',
                            },
                        },
                        ownerPayoutDetails: {
                            bankTransfer: undefined,
                            cardPayment: {
                                panMask: '*** 3432',
                            },
                        },
                        receipts: [],
                    },
                },
                contract: {
                    contractId: '97ab6be78cfe460fbf49ca1eff21bf45' as Flavor<string, 'ContractID'>,
                    rentStartDate: '2021-02-15T21:00:00Z',
                    ownerInfo: ownerPersonContract,
                    tenantInfo: tenantPersonContract,
                    rentAmount: '10000000',
                    paymentDayOfMonth: 14,
                    insurance: {
                        policyDate: '2020-12-10T21:00:00Z',
                        policyNumber: 'dsa123',
                        insuranceAmount: '500',
                    },
                    commissions: {
                        monthlyTenantCommission: '0',
                        monthlyOwnerCommission: '0.05',
                        oneTimeTenantCommission: '0',
                    },
                    calculatedAmounts: {
                        tenantPaymentAmount: '10000500',
                        ownerPaymentAmount: '10000000',
                        firstMonthPaymentAmount: '10000500',
                    },
                    housingAndCommunalServices: {
                        description: '',
                        meteringTransferDayOfMonth: 0,
                        comment: '12312312313',
                        paymentAgreement: '',
                    },
                    contractNumber: '3333',
                    houseInfo: {},
                    paymentsRegister: '231321321312',
                    status: RentContractStatus.ACTIVE,
                    calculationStrategy: 'STRATEGY_1',
                },
            },
        },
        managerFlat: {
            flat: {
                flatId: '597891f7f9314fd386c2850d6fb75d6e' as Flavor<string, 'FlatID'>,
            },
        },
        config: {
            serverTimeStamp: new Date('2021-03-16T21:00:00Z').getTime(),
        },
    });
};

export const unknownPayment = getStateCheckPayment(PaymentStatus.UNKNOWN);

export const futurePaymentPayment = getStateCheckPayment(
    '2021-02-15T21:00:00Z',
    '2021-03-15T21:00:00Z',
    '2021-03-01T21:00:00Z',
    PaymentStatus.FUTURE_PAYMENT
);

export const readyToPayPayment = getStateCheckPayment(
    '2021-03-28T21:00:00Z',
    '2021-02-28T21:00:00Z',
    '2021-03-20T21:00:00Z',
    PaymentStatus.NEW
);

export const todayPayment = getStateCheckPayment(
    '2021-04-28T21:00:00Z',
    '2021-02-28T21:00:00Z',
    '2021-04-28T23:00:00Z',
    PaymentStatus.NEW
);

export const outdatedPayment = getStateCheckPayment(
    '2021-03-28T21:00:00Z',
    '2021-02-28T21:00:00Z',
    '2021-03-29T22:00:00Z',
    PaymentStatus.NEW
);

export const paidByTenantPayment = getStateCheckPayment(
    '2021-03-15T21:00:00Z',
    '2021-02-15T21:00:00Z',
    '2021-03-16T21:00:00Z',
    PaymentStatus.PAID_BY_TENANT,
    PayoutStatus.UNKNOWN
);

export const paidByTenantByPhonePayment = getStateCheckPayment(
    '2021-03-15T21:00:00Z',
    '2021-02-15T21:00:00Z',
    '2021-03-16T21:00:00Z',
    PaymentStatus.PAID_BY_TENANT,
    PayoutStatus.UNKNOWN,
    undefined,
    undefined,
    {
        phonePayment: {
            panMask: '+7 (951) ***-**-72',
        },
    }
);

export const paidByTenantHoldingPayment = getStateCheckPayment(
    '2021-03-15T21:00:00Z',
    '2021-02-15T21:00:00Z',
    '2021-03-16T21:00:00Z',
    PaymentStatus.PAID_BY_TENANT,
    PayoutStatus.NOT_STARTED
);

export const payoutRetriesPayment = getStateCheckPayment(
    '2021-03-15T21:00:00Z',
    '2021-02-15T21:00:00Z',
    '2021-03-16T21:00:00Z',
    PaymentStatus.PAID_BY_TENANT,
    PayoutStatus.RECOVERABLE_ERROR
);

export const retriesLimitReachedPayment = getStateCheckPayment(
    '2021-03-28T21:00:00Z',
    '2021-02-28T21:00:00Z',
    '2021-03-20T21:00:00Z',
    PaymentStatus.PAID_BY_TENANT,
    PayoutStatus.UNRECOVERABLE_ERROR,
    PayoutError.RETRIES_LIMIT_REACHED
);

export const boundOwnerCardIsAbsentPayment = getStateCheckPayment(
    '2021-03-28T21:00:00Z',
    '2021-02-28T21:00:00Z',
    '2021-03-20T21:00:00Z',
    PaymentStatus.PAID_BY_TENANT,
    PayoutStatus.UNRECOVERABLE_ERROR,
    PayoutError.BOUND_OWNER_CARD_IS_ABSENT
);

export const boundOwnerCardIsInactivePayment = getStateCheckPayment(
    '2021-03-28T21:00:00Z',
    '2021-02-28T21:00:00Z',
    '2021-03-20T21:00:00Z',
    PaymentStatus.PAID_BY_TENANT,
    PayoutStatus.UNRECOVERABLE_ERROR,
    PayoutError.BOUND_OWNER_CARD_IS_INACTIVE
);

export const boundOwnerCardIsNotTheOnlyPayment = getStateCheckPayment(
    '2021-03-28T21:00:00Z',
    '2021-02-28T21:00:00Z',
    '2021-03-20T21:00:00Z',
    PaymentStatus.PAID_BY_TENANT,
    PayoutStatus.UNRECOVERABLE_ERROR,
    PayoutError.BOUND_OWNER_CARD_IS_NOT_THE_ONLY
);

export const paidToOwnerOnCardPayment = getStateCheckPayment(
    '2021-03-28T21:00:00Z',
    '2021-02-28T21:00:00Z',
    '2021-03-20T21:00:00Z',
    PaymentStatus.PAID_TO_OWNER,
    PayoutStatus.PAID_OUT,
    undefined,
    {
        bankTransfer: undefined,
        cardPayment: {
            panMask: '*** 3432',
        },
    }
);

export const paidToOwnerOnRequisitesPayment = getStateCheckPayment(
    '2021-03-28T21:00:00Z',
    '2021-02-28T21:00:00Z',
    '2021-03-20T21:00:00Z',
    PaymentStatus.PAID_TO_OWNER,
    PayoutStatus.PAID_OUT,
    undefined,
    {
        bankTransfer: {},
        cardPayment: undefined,
    }
);

export const noUserFioPayment = getStateCheckUser(
    {},
    {},
    {
        bankInfo: { accountNumber: '12311231231231231223', bic: '044444444' },
        inn: '380114872773',
        assignedUser: {
            userId: '1eb0ad962faf' as Flavor<string, 'UserId'>,
            phone: '+79992134916',
            email: '12312312@list.ru',
            person: {},
            userRole: FlatUserRole.OWNER,
        },
        person: {},
    },
    {
        assignedUser: {
            userId: '2eb0ad962fad' as Flavor<string, 'UserId'>,
            phone: '+79173645952',
            email: '13158@list.ru',
            person: {},
            userRole: FlatUserRole.TENANT,
        },
        person: {},
    }
);

export const differentUserDataPayment = getStateCheckUser(
    {
        name: 'Антон_ERR',
        surname: 'Антонов_ERR',
        patronymic: 'Антонович_ERR',
    },
    {
        name: 'Семен_ERR',
        surname: 'Семенов_ERR',
        patronymic: 'Семенович_ERR',
    },
    {
        bankInfo: { accountNumber: '12311231231231231223', bic: '044444444' },
        inn: '380114872773',
        assignedUser: {
            userId: '1eb0ad962faf' as Flavor<string, 'UserId'>,
            phone: '+79992134916',
            email: '12312312@list.ru',
            person: {
                name: 'Антон',
                surname: 'Антонов',
                patronymic: 'Антонович',
            },
            userRole: FlatUserRole.OWNER,
        },
        person: {
            name: 'Антон',
            surname: 'Антонов',
            patronymic: 'Антонович',
        },
    },
    {
        assignedUser: {
            person: {
                name: 'Семен',
                surname: 'Семенов',
                patronymic: 'Семенович',
            },
            userId: '2eb0ad962fad' as Flavor<string, 'UserId'>,
            userRole: FlatUserRole.TENANT,
        },
        person: {
            name: 'Семен',
            surname: 'Семенов',
            patronymic: 'Семенович',
        },
    }
);

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    managerFlatPayment: {
        paymentWithContract: {
            payment: {
                id: 'c17302963e49460cc5cb3daa606d4441' as Flavor<string, 'PaymentId'>,
                status: PaymentStatus.PAID_BY_TENANT,
                contract: {
                    contractId: '97ab6be78cfe460fbf49ca1eff21bf45' as Flavor<string, 'ContractID'>,
                    rentStartDate: '2021-02-15T21:00:00Z',
                    contractNumber: '3333',
                    ownerPerson: {
                        name: 'Антон',
                        surname: 'Антонов',
                        patronymic: 'Антонович',
                    },
                    tenantPerson: {
                        name: 'Семен',
                        surname: 'Семенов',
                        patronymic: 'Семенович',
                    },
                },
                tenantRentPayment: {
                    startTime: '2021-06-13T21:00:00Z',
                    endTime: '2021-07-12T21:00:00Z',
                    contractPaymentDate: '2021-03-15T21:00:00Z',
                },
                moderationSpecificPaymentInfo: {
                    tenantAmount: '10000500',
                    ownerAmount: '9500000',
                    receipts: [],
                },
            },
            contract: {
                contractId: '97ab6be78cfe460fbf49ca1eff21bf45' as Flavor<string, 'ContractID'>,
                rentStartDate: '2021-02-15T21:00:00Z',
                ownerInfo: {
                    person: {
                        name: 'Антон',
                        surname: 'Антонов',
                        patronymic: 'Антонович',
                    },
                    phone: '+79992134916',
                    inn: '380114872773',
                    email: '12312312@list.ru',
                    bankInfo: {
                        accountNumber: '12311231231231231223',
                        bic: '044444444',
                    },
                },
                tenantInfo: {
                    person: {
                        name: 'Семен',
                        surname: 'Семенов',
                        patronymic: 'Семенович',
                    },
                    phone: '+79992134916',
                    email: '12312312@list.ru',
                },
                rentAmount: '10000000',
                paymentDayOfMonth: 14,
                insurance: {
                    policyDate: '2020-12-10T21:00:00Z',
                    policyNumber: 'dsa123',
                    insuranceAmount: '500',
                },
                commissions: {
                    monthlyTenantCommission: 0,
                    commissionValue: Commission_ServiceCommissionValue.SEVEN,
                },
                calculatedAmounts: {
                    tenantPaymentAmount: '10000500',
                    ownerPaymentAmount: '10000000',
                    firstMonthPaymentAmount: '10000500',
                },
                contractNumber: '3333',
                status: RentContractStatus.ACTIVE,
                calculationStrategy: CalculationStrategyNamespace_CalculationStrategy.STRATEGY_1,
            },
        },
    },
    managerFlat: {
        flat: {
            flatId: '597891f7f9314fd386c2850d6fb75d6e' as Flavor<string, 'FlatID'>,
        },
    },
    config: {
        serverTimeStamp: new Date('2021-03-16T21:00:00Z').getTime(),
    },
};
