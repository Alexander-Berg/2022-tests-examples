import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import {
    BorrowerCategoryTypes,
    FlatOrApartmentTypes,
    FlatTypes,
    IMortgageProgram,
    IncomeConfirmationTypes,
    MortgageTypes,
    NationalityTypes,
    PayTypes,
    ProvisionTypes,
} from 'realty-core/types/mortgage/mortgageProgram';

const logoImgUrl = generateImageUrl({ width: 142, height: 33 });

export const getPrograms = (size = 3): IMortgageProgram[] =>
    [
        {
            id: 2389602,
            bank: {
                id: '386832',
                name: 'АТБ',
                logo: logoImgUrl,
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
                minRate: 10.4,
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
                discountRate: 0.1,
                specialCondition: ['их нет'],
                additionalDescription: ['их нет'],
            },
            monthlyPayment: '27271',
            partnerIntegrationType: [],
        },
        {
            id: 2389845,
            bank: {
                id: '386832',
                name: 'АТБ',
                logo: logoImgUrl,
            },
            programName: 'Военная ипотека',
            flatType: [FlatTypes.SECONDARY, FlatTypes.NEW_FLAT],
            flatOrApartment: [FlatOrApartmentTypes.FLAT],
            mortgageType: MortgageTypes.MILITARY,
            maternityCapital: false,
            requirements: {
                incomeConfirmation: [IncomeConfirmationTypes.WITHOUT_PROOF],
                borrowerCategory: [BorrowerCategoryTypes.EMPLOYEE],
                minAge: 25,
                minExperienceMonths: 36,
                documents: ['Документы'],
                requirements: ['Требования'],
                nationality: [NationalityTypes.RF],
            },
            creditParams: {
                minRate: 7.3,
                rateDescription: ['Военная ипотека.'],
                minDownPayment: 15,
                minAmount: '100000',
                maxAmount: '3310326',
                minPeriodYears: 3,
                maxPeriodYears: 25,
                payType: PayTypes.ANNUITY,
                provisionType: ProvisionTypes.PURCHASED,
            },
            monthlyPayment: '21819',
            partnerIntegrationType: [],
        },
    ].slice(0, size);
