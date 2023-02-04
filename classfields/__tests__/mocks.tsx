import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import {
    IMortgageProgram,
    FlatTypes,
    FlatOrApartmentTypes,
    MortgageTypes,
    IncomeConfirmationTypes,
    BorrowerCategoryTypes,
    NationalityTypes,
    PayTypes,
    ProvisionTypes,
    PartnerIntegrationTypes,
} from 'realty-core/types/mortgage/mortgageProgram';

export const getProgram = (): IMortgageProgram => ({
    id: 2389602,
    bank: {
        id: '386832',
        name: 'АТБ',
        logo: generateImageUrl({ width: 142, height: 33 }),
    },
    programName: 'Ипотека в поддержку молодых семей',
    flatType: [FlatTypes.SECONDARY],
    flatOrApartment: [FlatOrApartmentTypes.APARTMENT],
    mortgageType: MortgageTypes.YOUNG_FAMILY,
    maternityCapital: true,
    requirements: {
        incomeConfirmation: [IncomeConfirmationTypes.WITHOUT_PROOF, IncomeConfirmationTypes.PFR],
        borrowerCategory: [BorrowerCategoryTypes.EMPLOYEE],
        minAge: 20,
        maxAge: 50,
        minExperienceMonths: 10,
        totalExperienceMonths: 500,
        documents: ['Докис'],
        requirements: ['Требования'],
        nationality: [NationalityTypes.RF],
    },
    creditParams: {
        minRate: 10.5,
        specialRate: 5.5,
        validityPeriod: 12,
        rateDescription: ['описание'],
        increasingFactor: [
            {
                factor: 'Плохая кредитная история',
                rate: 5.5,
            },
            {
                factor: 'Отказ от страхования жизни',
                rate: 4.3,
            },
        ],
        reducingFactor: [
            {
                factor: 'Добряк',
                rate: 1,
            },
        ],
        minDownPayment: 45.5,
        minAmount: '1000000',
        maxAmount: '100000000',
        minPeriodYears: 10,
        maxPeriodYears: 25,
        payType: PayTypes.ANNUITY,
        provisionType: ProvisionTypes.PURCHASED,
        solutionPeriodMonths: 12,
        additionalDescription: ['их нет'],
    },
    monthlyPayment: '27271',
    partnerIntegrationType: [],
});

export const getProgramWithoutRequirements = (): IMortgageProgram => ({
    ...getProgram(),
    requirements: {
        incomeConfirmation: [],
    },
});

export const getProgramLargeMonthlyPayment = (): IMortgageProgram => ({
    ...getProgram(),
    monthlyPayment: '5399685',
});

export const getProgramAlfaIntegration = (): IMortgageProgram => ({
    ...getProgram(),
    partnerIntegrationType: [PartnerIntegrationTypes.ALFABANK_FRAME_FORM],
});

export const getProgramWithDiscount = (): IMortgageProgram => ({
    ...getProgram(),
    creditParams: {
        ...getProgram().creditParams,
        minRateWithDiscount: 10.4,
        discountRate: 0.1,
        specialCondition: ['их нет'],
    },
});
