import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import {
    IMortgageProgram,
    FlatOrApartmentTypes,
    FlatTypes,
    MortgageTypes,
    IncomeConfirmationTypes,
    BorrowerCategoryTypes,
    NationalityTypes,
    PayTypes,
    ProvisionTypes,
} from 'realty-core/types/mortgage/mortgageProgram';

interface IGenerateMortgageProgramParams {
    id: number;
    programName: string;
    minRate: number;
    minRateWithDiscount?: number;
    minPeriodYears: number;
    maxPeriodYears: number;
    monthlyPayment: string;
}

function generateMortgageProgram({
    id,
    programName,
    minRate,
    minRateWithDiscount,
    minPeriodYears,
    maxPeriodYears,
    monthlyPayment,
}: IGenerateMortgageProgramParams): IMortgageProgram {
    return {
        id,
        bank: {
            id: '323579',
            name: 'Банк Открытие',
            logo: generateImageUrl({
                width: 96,
                height: 20,
            }),
        },
        programName,
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
            minRate,
            minRateWithDiscount,
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
            minPeriodYears,
            maxPeriodYears,
            payType: PayTypes.ANNUITY,
            provisionType: ProvisionTypes.PURCHASED,
            solutionPeriodMonths: 3.0,
            additionalDescription: [''],
        },
        monthlyPayment,
        partnerIntegrationType: [],
    };
}

export const fivePrograms = [
    generateMortgageProgram({
        id: 1,
        programName: 'Ипотека в поддержку молодых семей',
        minRate: 10.5,
        minRateWithDiscount: 10.4,
        minPeriodYears: 10,
        maxPeriodYears: 25,
        monthlyPayment: '27271',
    }),
    generateMortgageProgram({
        id: 2,
        programName: 'Военная ипотека',
        minRate: 7.3,
        minPeriodYears: 3,
        maxPeriodYears: 25,
        monthlyPayment: '21819',
    }),
    generateMortgageProgram({
        id: 3,
        programName: 'Ипотека для Дальнего Востока',
        minRate: 1.6,
        minPeriodYears: 3,
        maxPeriodYears: 20,
        monthlyPayment: '13379',
    }),
    generateMortgageProgram({
        id: 4,
        programName: 'Ипотека на новостройки',
        minRate: 7.84,
        minPeriodYears: 12,
        maxPeriodYears: 30,
        monthlyPayment: '22729',
    }),
    generateMortgageProgram({
        id: 5,
        programName: 'Новостройка с господдержкой 2020',
        minRate: 6.25,
        minPeriodYears: 1,
        maxPeriodYears: 30,
        monthlyPayment: '20101',
    }),
];

export const twentyPrograms = [...fivePrograms, ...fivePrograms, ...fivePrograms, ...fivePrograms];
