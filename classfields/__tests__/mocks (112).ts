import {
    FlatOrApartmentTypes,
    FlatTypes,
    IMortgageProgram,
    IncomeConfirmationTypes,
    MortgageTypes,
    PayTypes,
    ProvisionTypes,
} from 'realty-core/types/mortgage/mortgageProgram';

export const firstProgram: IMortgageProgram = {
    id: 2390797,
    bank: {
        id: '323579',
        name: 'Банк Открытие',
    },
    programName: 'Семейная ипотека',
    flatType: [FlatTypes.NEW_FLAT],
    flatOrApartment: [FlatOrApartmentTypes.FLAT],
    mortgageType: MortgageTypes.STATE_SUPPORT,
    maternityCapital: false,
    requirements: {
        incomeConfirmation: [IncomeConfirmationTypes.PFR, IncomeConfirmationTypes.WITHOUT_PROOF],
    },
    creditParams: {
        minRate: 4.7,
        rateDescription: ['Ставка 4.7% годовых действует при выполнении условий.'],
        minDownPayment: 15.0,
        minAmount: '500000',
        maxAmount: '12000000',
        minPeriodYears: 3,
        maxPeriodYears: 30,
        payType: PayTypes.ANNUITY,
        provisionType: ProvisionTypes.PURCHASED,
    },
    monthlyPayment: '27271',
    partnerIntegrationType: [],
};

export const secondProgram: IMortgageProgram = {
    id: 2390797,
    bank: {
        id: '323579',
        name: 'АТБ',
    },
    programName: 'Семейная ипотека',
    flatType: [FlatTypes.NEW_FLAT, FlatTypes.SECONDARY],
    flatOrApartment: [FlatOrApartmentTypes.FLAT, FlatOrApartmentTypes.APARTMENT],
    mortgageType: MortgageTypes.STATE_SUPPORT,
    maternityCapital: false,
    requirements: {
        incomeConfirmation: [IncomeConfirmationTypes.WITHOUT_PROOF],
        minAge: 21,
        maxAge: 70,
    },
    creditParams: {
        minRate: 4.7,
        minRateWithDiscount: 4.3,
        discountRate: 0.4,
        rateDescription: ['Ставка 4.3% годовых действует при выполнении условий.'],
        minDownPayment: 15.0,
        minAmount: '500000',
        maxAmount: '12000000',
        minPeriodYears: 3,
        maxPeriodYears: 30,
        payType: PayTypes.ANNUITY,
        provisionType: ProvisionTypes.PURCHASED,
    },
    monthlyPayment: '27271',
    partnerIntegrationType: [],
};
