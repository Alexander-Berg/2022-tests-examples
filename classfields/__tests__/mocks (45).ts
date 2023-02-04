import {
    BorrowerCategoryTypes,
    FlatTypes,
    IncomeConfirmationTypes,
    MortgageTypes,
} from 'realty-core/types/mortgage/mortgageProgramsFilters';

export const getSliderValues = (diff = 0) => ({
    propertyCost: 6500000 + diff,
    downPaymentSum: 1950000 + diff,
    periodYears: 25 + diff,
});

export const getSliderLimits = () => ({
    minPropertyCost: 700000,
    maxPropertyCost: 16500000,
    minDownPayment: 10,
    maxDownPayment: 50,
    minDownPaymentSum: 975000,
    maxDownPaymentSum: 6500000,
    minPeriodYears: 2,
    maxPeriodYears: 30,
    minCreditAmount: 500000,
    maxCreditAmount: 30000000,
    minRate: 1,
    maxRate: 15,
});

export const getPageParams = () => ({
    ...getSliderValues(),
    flatType: [FlatTypes.SECONDARY],
    mortgageType: [MortgageTypes.MILITARY, MortgageTypes.STATE_SUPPORT],
    bankId: ['1'],
    minRate: 5,
    maxRate: 10,
    borrowerCategory: [BorrowerCategoryTypes.INDIVIDUAL_ENTREPRENEUR, BorrowerCategoryTypes.BUSINESS_OWNER],
    incomeConfirmationType: IncomeConfirmationTypes.REFERENCE_2NDFL,
    maternityCapital: 'YES' as const,
});

export const getBanks = () => [
    {
        id: 1,
        name: 'Сбербанк',
    },
];
