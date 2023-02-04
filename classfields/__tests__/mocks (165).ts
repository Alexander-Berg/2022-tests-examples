import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import {
    BorrowerCategoryTypes,
    FlatOrApartmentTypes,
    FlatTypes,
    IMortgageProgram,
    IncomeConfirmationTypes,
    MortgageTypes,
    NationalityTypes,
    PartnerIntegrationTypes,
    PayTypes,
    ProvisionTypes,
} from 'realty-core/types/mortgage/mortgageProgram';

export const simpleProgram: IMortgageProgram = {
    id: 2390797,
    bank: {
        id: '323579',
        name: 'Банк Открытие',
        logo: generateImageUrl({
            width: 96,
            height: 20,
        }),
    },
    programName: 'Семейная ипотека',
    flatType: [FlatTypes.NEW_FLAT, FlatTypes.SECONDARY],
    flatOrApartment: [FlatOrApartmentTypes.FLAT],
    mortgageType: MortgageTypes.STATE_SUPPORT,
    maternityCapital: false,
    requirements: {
        incomeConfirmation: [
            IncomeConfirmationTypes.PFR,
            IncomeConfirmationTypes.REFERENCE_2NDFL,
            IncomeConfirmationTypes.BANK_REFERENCE,
        ],
        borrowerCategory: [
            BorrowerCategoryTypes.BUSINESS_OWNER,
            BorrowerCategoryTypes.INDIVIDUAL_ENTREPRENEUR,
            BorrowerCategoryTypes.EMPLOYEE,
        ],
        minAge: 18,
        maxAge: 70,
        minExperienceMonths: 3,
        totalExperienceMonths: 12,
        documents: ['Выписка из домовой книги (вторичный рынок).'],
        requirements: [
            'Многоквартирный дом/Нежилое здание должно находиться на территории г. Москвы, г. Санкт-Петербурга.',
        ],
        nationality: [NationalityTypes.RF],
    },
    creditParams: {
        minRate: 4.7,
        rateDescription: ['Ставка 4.7% годовых действует при выполнении условий.'],
        increasingFactor: [
            {
                factor: 'Непредоставление банку документов об обременении в пользу банка',
                rate: 2.0,
            },
            {
                factor: 'Отсутствие личного страхования при приобретении жилья на территории РФ.',
                rate: 1.3,
            },
            {
                factor: 'Отсутствие личного страхования при приобретении жилья на Дальнем Востоке',
                rate: 0.3,
            },
        ],
        minDownPayment: 15.0,
        minAmount: '500000',
        maxAmount: '12000000',
        minPeriodYears: 3,
        maxPeriodYears: 30,
        payType: PayTypes.ANNUITY,
        provisionType: ProvisionTypes.PURCHASED,
        solutionPeriodMonths: 3.0,
        additionalDescription: [''],
    },
    monthlyPayment: '27271',
    partnerIntegrationType: [],
};

export const longNameProgram: IMortgageProgram = {
    ...simpleProgram,
    programName: 'Ипотека с господдержкой для семей',
};

export const largeMonthlyPaymentProgram: IMortgageProgram = {
    ...simpleProgram,
    monthlyPayment: '2329391',
};

export const programWithoutMinExperience: IMortgageProgram = {
    ...simpleProgram,
    requirements: {
        ...simpleProgram.requirements,
        minExperienceMonths: undefined,
    },
};

export const programWithoutExperience: IMortgageProgram = {
    ...simpleProgram,
    requirements: {
        ...simpleProgram.requirements,
        minExperienceMonths: undefined,
        totalExperienceMonths: undefined,
    },
};

export const programWithAlfaIntegration: IMortgageProgram = {
    ...simpleProgram,
    partnerIntegrationType: [PartnerIntegrationTypes.ALFABANK_FRAME_FORM],
};

export const programWithDiscount: IMortgageProgram = {
    ...simpleProgram,
    programName: 'Ипотека с господдержкой для семей',
    creditParams: {
        ...simpleProgram.creditParams,
        minRateWithDiscount: 4.1,
        discountRate: 0.6,
        specialCondition: [
            'Базовая ставка банка для некоторых видов кредитов будет снижена на 0,6%.',
            'Оформите заявку на ипотеку на нашем сервисе и получите более выгодные условия.',
        ],
    },
};

export const programWithBankUrl = {
    ...simpleProgram,
    descriptionUrl: 'https://www.atb.su/kredit/ipoteka/kalkulyator/ipoteka-s-gos-podderzhkoy/',
};

export const programWithVeryLongName = {
    ...simpleProgram,
    programName: 'Военная ипотека. Кредит Семейный. Семейная ипотека ГК ПИК',
};
