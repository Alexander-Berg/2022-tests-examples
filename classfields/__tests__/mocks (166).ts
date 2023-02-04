import { DeepPartial } from 'utility-types';
import omit from 'lodash/omit';

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
import { IMortgageProgramsStore } from 'realty-core/view/react/modules/mortgage/mortgage-programs/redux/reducer';
import { IMortgageBankList } from 'realty-core/types/mortgage/mortgageBank';

import { IMortgageStore } from 'view/react/deskpad/reducers/roots/mortgage';

export function getMockGate(programs: number[] = [], programsAmount: number[] = []) {
    return class Gate {
        static getProgramsCounter = -1;
        static getProgramsCountCounter = 0;

        static get(path: string, params: Record<string, unknown>) {
            switch (path) {
                case 'react-page.get': {
                    if (Gate.getProgramsCounter === -1) {
                        Gate.getProgramsCounter++;
                        return Promise.resolve({});
                    }

                    const searchQuery = omit(params, ['rgid', '_pageType', '_providers', 'crc']);

                    return Promise.resolve({
                        mortgagePrograms: {
                            ...getStore({
                                searchQuery: { ...defaultSearchQuery, ...searchQuery },
                                programsAmount: programs[Gate.getProgramsCounter++],
                            }).mortgagePrograms,
                        },
                    });
                }
                case 'mortgage.getProgramsCount': {
                    return Promise.resolve({
                        mortgageProgramCount: programsAmount[Gate.getProgramsCountCounter++] || 0,
                    });
                }
                case 'search.getOffers': {
                    return Promise.resolve({});
                }
                default: {
                    return Promise.resolve({});
                }
            }
        }
    };
}

const defaultSearchQuery = {
    propertyCost: 3000000,
    downPaymentSum: 900000,
    periodYears: 15,
};

const calculatorLimits = {
    minCreditAmount: 300000,
    maxCreditAmount: 30000000,
    minPropertyCost: 333334,
    maxPropertyCost: 50000000,
    minPeriodYears: 10,
    maxPeriodYears: 30,
    minDownPayment: 10,
    minDownPaymentSum: 33334,
    maxDownPayment: 100,
    maxDownPaymentSum: 50000000,
    minRate: 1,
    maxRate: 15,
};

function getPrograms(
    amount: number,
    params: { propertyCost: number; downPaymentSum: number; periodYears: number } = defaultSearchQuery
): IMortgageProgram[] {
    const programNames = [
        'Ипотека на строящееся жилье',
        'Ипотека на готовое жилье',
        'Ипотека с господдержкой',
        'Военная ипотека',
        'Ипотека для Дальнего Востока',
        'Ипотека с гос. поддержкой',
        'Семейная ипотека',
        'Приобретение квартиры на этапе строительства',
        'Приобретение готового жилья',
        'Ипотека на новостройки',
    ];

    const rates = [7.89, 7.99, 5.99, 7.3, 1.6, 5.99, 4.7, 7.6, 7.3, 7.84];

    return Array(amount)
        .fill(null)
        .map((_, index) => {
            const rate = rates[index % rates.length];

            const monthPeriod = params.periodYears * 12;
            const monthRate = rate / 1200;
            const commonRate = Math.pow(1 + monthRate, monthPeriod);

            const monthlyPayment = Math.round(
                ((monthRate * commonRate) / (commonRate - 1)) * (params.propertyCost - params.downPaymentSum)
            );

            return {
                id: index,
                bank: {
                    id: '323579',
                    name: 'Банк Открытие',
                    logo: generateImageUrl({
                        width: 96,
                        height: 20,
                    }),
                },
                programName: programNames[index % programNames.length],
                flatType: [
                    ...(index % 2 === 0 ? [FlatTypes.NEW_FLAT] : []),
                    ...(index % 3 === 0 ? [FlatTypes.SECONDARY] : []),
                ],
                flatOrApartment: [FlatOrApartmentTypes.FLAT],
                mortgageType: MortgageTypes[Object.keys(MortgageTypes)[index % Object.keys(MortgageTypes).length]],
                maternityCapital: index % 2 === 0,
                requirements: {
                    incomeConfirmation: [],
                    borrowerCategory: [],
                    minAge: 18,
                    maxAge: 70,
                    minExperienceMonths: 3,
                    totalExperienceMonths: 12,
                    documents: [],
                    requirements: [],
                    nationality: [NationalityTypes.RF],
                },
                creditParams: {
                    minRate: rate,
                    rateDescription: [],
                    increasingFactor: [],
                    minDownPayment: 15.0,
                    minAmount: '500000',
                    maxAmount: '12000000',
                    minPeriodYears: 3,
                    maxPeriodYears: 30,
                    payType: PayTypes.ANNUITY,
                    provisionType: ProvisionTypes.PURCHASED,
                    solutionPeriodMonths: 3.0,
                    additionalDescription: [],
                },
                monthlyPayment: monthlyPayment.toString(),
                partnerIntegrationType: [],
            };
        });
}

function getBanks(count: number): IMortgageBankList[] {
    return Array(count)
        .fill(null)
        .map((_, index) => {
            return {
                id: index,
                name: `bank_${index}`,
                logo: generateImageUrl({
                    width: 110,
                    height: 26,
                }),
            };
        });
}

export function getStore({
    searchQuery,
    programsAmount,
}: {
    searchQuery?: IMortgageProgramsStore['searchQuery'];
    programsAmount?: number;
} = {}): DeepPartial<IMortgageStore> {
    const totalItems = programsAmount || 25;

    return {
        ads: {},
        user: {},
        geo: {
            rgid: 1,
            id: 2,
            type: 'SUBJECT_FEDERATION',
            name: 'Санкт-Петербург и ЛО',
            locative: 'в Санкт-Петербурге и ЛО',
        },
        page: { name: 'mortgage-search' },
        mortgagePrograms: {
            items: getPrograms(totalItems < 20 ? totalItems : 20, searchQuery || defaultSearchQuery),
            pager: {
                totalItems,
                page: 0,
                pageSize: 20,
            },
            isMoreLoading: false,
            queryId: '60661c0c4238caabcd8d9c512cbe7039',
            calculatorLimits,
            defaultCalculatorLimits: calculatorLimits,
            searchQuery: searchQuery || defaultSearchQuery,
            defaultSearchQuery,
            banks: getBanks(20),
            filtersCount: totalItems,
        },
    };
}

export const fullSearchQuery: IMortgageProgramsStore['searchQuery'] = {
    mortgageType: [MortgageTypes.YOUNG_FAMILY],
    incomeConfirmationType: IncomeConfirmationTypes.WITHOUT_PROOF,
    borrowerCategory: [BorrowerCategoryTypes.EMPLOYEE],
    propertyCost: 13210000,
    downPaymentSum: 6430000,
    periodYears: 10,
    regionId: 1,
};
