import { DeepPartial } from 'utility-types';

import { Flavor } from 'realty-core/types/utils';

import {
    NaturalPersonCheckResolution,
    PersonCheckStatus,
    PersonCheckResulution,
    NaturalPersonCheckStatus,
    NaturalPersonCheckType,
    ISpectrumReport,
    RecordStatus,
} from 'types/userChecks';

import { TenantQuestionnaireModerationStatus } from 'types/user';

import { IUniversalStore } from 'view/modules/types';

export const storeAllSuccess: DeepPartial<Pick<IUniversalStore, 'managerUser'>> = {
    managerUser: {
        user: {
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.VALID,
            tenantQuestionnaire: {
                appliedPromocode: 'Promocode',
            },
        },
        naturalPersonChecks: {
            resolution: NaturalPersonCheckResolution.VALID,
            status: NaturalPersonCheckStatus.READY,
            checks: [
                {
                    [NaturalPersonCheckType.PASSPORT_ACTUAL]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.PASSPORT_VERIFICATION]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.WANTED]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.EXTREMIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.FSSP_DEBT]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.NATURAL_PERSON_INN]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.BLACK_LIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
            ],
        },
    },
};

export const storeAllInProgress: DeepPartial<Pick<IUniversalStore, 'managerUser'>> = {
    managerUser: {
        user: {
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
        },
        naturalPersonChecks: {
            resolution: NaturalPersonCheckResolution.VALID,
            status: NaturalPersonCheckStatus.IN_PROGRESS,
            checks: [
                {
                    [NaturalPersonCheckType.PASSPORT_ACTUAL]: {},
                    status: PersonCheckStatus.IN_PROGRESS,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.PASSPORT_VERIFICATION]: {},
                    status: PersonCheckStatus.IN_PROGRESS,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.WANTED]: {},
                    status: PersonCheckStatus.IN_PROGRESS,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.EXTREMIST]: {},
                    status: PersonCheckStatus.IN_PROGRESS,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.FSSP_DEBT]: {},
                    status: PersonCheckStatus.IN_PROGRESS,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.NATURAL_PERSON_INN]: {},
                    status: PersonCheckStatus.IN_PROGRESS,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.BLACK_LIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
            ],
        },
    },
};

export const storeAllInvalid: DeepPartial<Pick<IUniversalStore, 'managerUser'>> = {
    managerUser: {
        user: {
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.INVALID,
        },
        naturalPersonChecks: {
            resolution: NaturalPersonCheckResolution.INVALID,
            status: NaturalPersonCheckStatus.READY,
            checks: [
                {
                    [NaturalPersonCheckType.PASSPORT_ACTUAL]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.PASSPORT_VERIFICATION]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.WANTED]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.EXTREMIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.FSSP_DEBT]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.NATURAL_PERSON_INN]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.BLACK_LIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
            ],
        },
    },
};

export const storeErrors: DeepPartial<Pick<IUniversalStore, 'managerUser'>> = {
    managerUser: {
        user: {
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.INVALID,
        },
        naturalPersonChecks: {
            resolution: NaturalPersonCheckResolution.INVALID,
            status: NaturalPersonCheckStatus.ERROR,
            checks: [
                {
                    [NaturalPersonCheckType.PASSPORT_ACTUAL]: {},
                    status: PersonCheckStatus.ERROR,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.PASSPORT_VERIFICATION]: {},
                    status: PersonCheckStatus.ERROR,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.WANTED]: {},
                    status: PersonCheckStatus.ERROR,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.EXTREMIST]: {},
                    status: PersonCheckStatus.ERROR,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.FSSP_DEBT]: {},
                    status: PersonCheckStatus.ERROR,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.NATURAL_PERSON_INN]: {},
                    status: PersonCheckStatus.ERROR,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.BLACK_LIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
            ],
        },
    },
};

export const storeNoChecks: DeepPartial<Pick<IUniversalStore, 'managerUser'>> = {
    managerUser: {
        user: {},
        naturalPersonChecks: {
            checks: [],
        },
    },
};

export const storeExtremist: DeepPartial<Pick<IUniversalStore, 'managerUser' | 'managerUserChecks'>> = {
    managerUser: {
        user: {
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.INVALID,
        },
        naturalPersonChecks: {
            resolution: NaturalPersonCheckResolution.INVALID,
            status: NaturalPersonCheckStatus.READY,
            checks: [
                {
                    [NaturalPersonCheckType.PASSPORT_ACTUAL]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.PASSPORT_VERIFICATION]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.WANTED]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.EXTREMIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.FSSP_DEBT]: {
                        palmaReportId: '13ff508d9a97470ca18c2ec12b1a711b' as Flavor<string, 'PalmaReportId'>,
                    },
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.NATURAL_PERSON_INN]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.BLACK_LIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
            ],
        },
    },
};

export const storeExtremistGate: DeepPartial<Pick<ISpectrumReport, 'reportUid' | 'proceedingExecutiveReport'>> = {
    reportUid: '13ff508d9a97470ca18c2ec12b1a711b',
    proceedingExecutiveReport: {
        items: [
            {
                name: 'АБАКАРОВ МАГОМЕДАЛИ КАМАЛУТДИНОВИЧ',
                birthDate: new Date('1974-09-28T21:00:00Z'),
                birthPlace: ',СССР,ДАССР,ЛЕНИНСКИЙ РАЙОН,,П.МАНАС, ,,,',
                executiveProceedingNumber: '33582/18/05042-ИП',
                unloadStatus: RecordStatus.UPDATED,
                executiveDocumentNumber: 'ФС 0231511177',
                executiveDocumentType: 3,
                executiveDocumentTypeName: 'Акт по делу об административном правонарушении',
                executiveDocumentDate: new Date('2041-04-04T21:00:00Z'),
                initiationDate: new Date('2018-09-30T21:00:00Z'),
                endDate: new Date('1799-12-31T21:29:43Z'),
                executionSubjectType: 110,
                executionSubjectTypeName: 'Материальный ущерб по ГК РФ, причиненный физическим или юридическим лицам',
                issuer: 'КАРАБУДАХКЕНТСКИЙ РАЙОННЫЙ СУД',
                bailiffOfficeCode: 5042,
                bailiffOfficeAddress: '368530, Карабудахкентский район, с.Карабудахкент, ул.Коркмасова, д. 3',
                bailiffName: 'АБДУЛЛАТИПОВ Р. И.',
                bailiffPhone: '+7(87232)2-24-22',
                debtBalancePrincipal: 33834,
                debtBalanceDuty: 2368.38,
            },
            {
                name: 'АБАКАРОВ МАГОМЕДАЛИ КАМАЛУТДИНОВИЧ',
                birthDate: new Date('1974-09-28T21:00:00Z'),
                birthPlace: ',СССР,ДАССР,ЛЕНИНСКИЙ РАЙОН,,П.МАНАС, ,,,',
                executiveProceedingNumber: '89360/19/05030-ИП',
                unloadStatus: RecordStatus.APPARENTLY_CLOSED,
                executiveDocumentNumber: '18810005190001541932',
                executiveDocumentType: 3,
                executiveDocumentTypeName: 'Акт по делу об административном правонарушении',
                executiveDocumentDate: new Date('2019-06-09T21:00:00Z'),
                initiationDate: new Date('2019-09-02T21:00:00Z'),
                endDate: new Date('1799-12-31T21:29:43Z'),
                executionSubjectType: 37,
                executionSubjectTypeName: 'Штраф ГИБДД',
                issuer:
                    // eslint-disable-next-line max-len
                    'ПОЛК ДПС ГИБДД МВД ПО РЕСПУБЛИКЕ ДАГЕСТАН (БАТАЛЬОН ПО ОБСЛУЖИВАНИЮ ФЕДЕРАЛЬНЫХ АВТОМОБИЛЬНЫХ ДОРОГ)',
                bailiffOfficeCode: 5030,
                bailiffOfficeAddress: '368222, Россия, Респ. Дагестан, , г. Буйнакск, , ул. М. Атаева, д. 29, ,',
                bailiffName: 'ОМАРОВ М. М.',
                bailiffPhone: '+7(87237)2-38-45',
                debtBalancePrincipal: 1056.34,
                debtBalanceDuty: 2218.37,
            },
        ],
    },
};

export const storeWantedError: DeepPartial<Pick<IUniversalStore, 'managerUser'>> = {
    managerUser: {
        user: {
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.VALID,
        },
        naturalPersonChecks: {
            resolution: NaturalPersonCheckResolution.INVALID,
            status: NaturalPersonCheckStatus.READY,
            checks: [
                {
                    [NaturalPersonCheckType.PASSPORT_ACTUAL]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.PASSPORT_VERIFICATION]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.WANTED]: {},
                    status: PersonCheckStatus.ERROR,
                },
                {
                    [NaturalPersonCheckType.EXTREMIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.FSSP_DEBT]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.NATURAL_PERSON_INN]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.BLACK_LIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
            ],
        },
    },
};

export const storeWantedErrorGate: DeepPartial<Pick<IUniversalStore, 'managerUser'>> = {
    managerUser: {
        user: {
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.VALID,
        },
        naturalPersonChecks: {
            resolution: NaturalPersonCheckResolution.INVALID,
            status: NaturalPersonCheckStatus.READY,
            checks: [
                {
                    [NaturalPersonCheckType.PASSPORT_ACTUAL]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.PASSPORT_VERIFICATION]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.WANTED]: {},
                    status: PersonCheckStatus.TO_BE_REQUESTED,
                },
                {
                    [NaturalPersonCheckType.EXTREMIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.FSSP_DEBT]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.NATURAL_PERSON_INN]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.BLACK_LIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
            ],
        },
    },
};

export const storePassportInvalid: DeepPartial<Pick<IUniversalStore, 'managerUser' | 'modal'>> = {
    managerUser: {
        user: {
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.VALID,
        },
        naturalPersonChecks: {
            resolution: NaturalPersonCheckResolution.INVALID,
            status: NaturalPersonCheckStatus.READY,
            checks: [
                {
                    [NaturalPersonCheckType.PASSPORT_ACTUAL]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.PASSPORT_VERIFICATION]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.WANTED]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.EXTREMIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.FSSP_DEBT]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.NATURAL_PERSON_INN]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.BLACK_LIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
            ],
        },
    },
};

export const storePassportInvalidGate: DeepPartial<Pick<IUniversalStore, 'managerUser'>> = {
    managerUser: {
        user: {
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.VALID,
        },
        naturalPersonChecks: {
            resolution: NaturalPersonCheckResolution.INVALID,
            status: NaturalPersonCheckStatus.READY,
            checks: [
                {
                    [NaturalPersonCheckType.PASSPORT_ACTUAL]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.PASSPORT_VERIFICATION]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.WANTED]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.EXTREMIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.FSSP_DEBT]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.NATURAL_PERSON_INN]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.BLACK_LIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
            ],
        },
    },
};

export const storeMultipleErrors: DeepPartial<Pick<IUniversalStore, 'managerUser' | 'modal'>> = {
    managerUser: {
        user: {
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.VALID,
        },
        naturalPersonChecks: {
            resolution: NaturalPersonCheckResolution.INVALID,
            status: NaturalPersonCheckStatus.READY,
            checks: [
                {
                    [NaturalPersonCheckType.PASSPORT_ACTUAL]: {},
                    status: PersonCheckStatus.ERROR,
                },
                {
                    [NaturalPersonCheckType.PASSPORT_VERIFICATION]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.INVALID,
                },
                {
                    [NaturalPersonCheckType.WANTED]: {},
                    status: PersonCheckStatus.ERROR,
                },
                {
                    [NaturalPersonCheckType.EXTREMIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.FSSP_DEBT]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.NATURAL_PERSON_INN]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
                {
                    [NaturalPersonCheckType.BLACK_LIST]: {},
                    status: PersonCheckStatus.READY,
                    resolution: PersonCheckResulution.VALID,
                },
            ],
        },
    },
};
