import { HOST } from 'common/utils/test-utils/common';

export const contractClasses = {
    request: {
        data: {
            enum_code: 'CONTRACT_CLASS'
        },
        url: `${HOST}/common/enums`
    },
    response: [
        { id: 'PARTNERS', label: 'РСЯ' },
        { id: 'DISTRIBUTION', label: 'Дистрибуция' },
        { id: 'AFISHA', label: 'Афиша' }
    ]
};

export const firms = {
    request: {
        url: `${HOST}/firm/list`
    },
    response: [
        { id: -1, label: 'Фирма не определена' },
        { id: 1, label: 'ООО «Яндекс»' },
        { id: 2, label: 'ООО «Яндекс.Украина»' },
        { id: 3, label: 'ТОО «KazNet Media (КазНет Медиа)»' }
    ]
};

export const services = {
    request: {
        url: `${HOST}/service/list`,
        data: {}
    },
    response: [
        {
            cc: 'adfox',
            url_orders: 'http://adfox.ru',
            in_contract: true,
            id: 102,
            name: 'ADFox.ru'
        },
        {
            cc: 'afisha_moviepass',
            url_orders: null,
            in_contract: true,
            id: 617,
            name: 'Afisha.MoviePass'
        }
    ]
};

export const partnerContractTypes = {
    request: {
        data: {
            enum_code: 'PARTNER_C_TYPE'
        },
        url: `${HOST}/common/enums`
    },
    response: [
        { id: '2', label: 'С Агрегатором' },
        { id: '3', label: 'С Физ. лицом' },
        { id: '1', label: 'С Юр. лицом' }
    ]
};

export const distributionContractTypes = {
    request: {
        data: {
            enum_code: 'DISTRIBUTION_C_TYPE'
        },
        url: `${HOST}/common/enums`
    },
    response: [
        { id: '1', label: 'Разделение доходов' },
        { id: '2', label: 'Загрузки/Установки' },
        { id: '3', label: 'Универсальный' }
    ]
};

export const intercompanies = {
    request: {
        url: `${HOST}/firm/intercompany_list`
    },
    response: [
        {
            cc: 'adfox',
            url_orders: 'http://adfox.ru',
            in_contract: true,
            id: 102,
            name: 'ADFox.ru'
        },
        {
            cc: 'afisha_moviepass',
            url_orders: null,
            in_contract: true,
            id: 617,
            name: 'Afisha.MoviePass'
        }
    ]
};

export const personTypes = {
    request: {
        url: `${HOST}/person/category/list`
    },
    response: [
        {
            category: 'am_jp',
            name: 'ID_Legal_entity_AM'
        },
        {
            category: 'am_np',
            name: 'ID_Individual_AM'
        },
        {
            category: 'az_ur',
            name: 'ID_Legal_entity_AZ'
        }
    ]
};

export const docSets = {
    request: {
        data: {
            enum_code: 'CONTRACT_DOC_SET'
        },
        url: `${HOST}/common/enums`
    },
    response: [
        { id: '1', label: 'старый (на оказание услуг)' },
        { id: '2', label: 'с фев. 2007 (на предоставление прав)' }
    ]
};

export const billIntervals = {
    request: {
        data: {
            enum_code: 'BILL_INTERVALS'
        },
        url: `${HOST}/common/enums`
    },
    response: [
        { id: '1', label: 'Акт раз в месяц' },
        { id: '2', label: 'Акт раз в квартал' }
    ]
};

export const platformTypes = {
    request: {
        data: {
            enum_code: 'PLATFORM_TYPE'
        },
        url: `${HOST}/common/enums`
    },
    response: [
        { id: null, label: 'выберите тип платформы...' },
        { id: '1', label: 'Мобильный' },
        { id: '2', label: 'Десктопный' }
    ]
};

export const partnerContracts = {
    request: {
        data: {
            bill_intervals: '1',
            contract_class: 'AFISHA',
            contract_eid: '1234',
            contract_eid_like: true,
            date_type: 'FINISHED',
            doc_set: '1',
            dt_from: '2020-06-02T00:00:00',
            dt_to: '2020-06-03T00:00:00',
            firm_id: 'apiKeys',
            pagination_pn: 1,
            pagination_ps: 10,
            sort_key: 'START_DT',
            sort_order: 'ASC'
        },
        url: `${HOST}/contract/partner-contracts`
    },
    response: {
        items: [
            {
                sentDt: null,
                contractClass: 'PARTNERS',
                currency: 643,
                contractType: 5,
                rewardType: 1,
                partnerPct: 43,
                clientName: 'balance_test 2020-09-04 01:30:02.303454',
                managerCode: 20160,
                isSigned: '2020-09-04T00:00:00',
                clientLogin: null,
                personId: 12491308,
                firm: 1,
                finishDt: '2020-09-11T00:00:00',
                testMode: null,
                personName: 'Юр. лицо РСЯRDWF Владимиров',
                contractEid: 'РСЯ-144741-09/20',
                isBookedDt: null,
                docSet: null,
                tagId: null,
                managerName: 'Нигай Елена Сергеевна',
                isFaxed: null,
                billIntervals: 1,
                clientId: 1339103779,
                services: null,
                contractId: 2148180,
                isOffer: null,
                platformType: null,
                tagName: null,
                serviceId: null,
                startDt: '2020-09-04T00:00:00',
                isCancelled: '2099-01-01T00:00:00'
            },
            {
                sentDt: null,
                contractClass: 'PARTNERS',
                currency: 643,
                contractType: 1,
                rewardType: 1,
                partnerPct: 45,
                clientName: 'createdAgency_XSCTH',
                managerCode: 20160,
                isSigned: '2020-09-04T00:00:00',
                clientLogin: null,
                personId: 12493573,
                firm: 1,
                finishDt: '2021-09-04T00:00:00',
                testMode: null,
                personName: 'Юр. лицо РСЯEzmv',
                contractEid: 'РС-144757-09/20',
                isBookedDt: null,
                docSet: 4,
                tagId: null,
                managerName: 'Нигай Елена Сергеевна',
                isFaxed: '2020-09-04T00:00:00',
                billIntervals: 1,
                clientId: 1339108271,
                services: null,
                contractId: 2149595,
                isOffer: null,
                platformType: null,
                tagName: null,
                serviceId: null,
                startDt: '2020-09-04T00:00:00',
                isCancelled: '2020-09-04T00:00:00'
            }
        ],
        totalRowCount: 2
    }
};
