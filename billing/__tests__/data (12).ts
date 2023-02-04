import { HOST } from 'common/utils/test-utils/common';

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

export const contracts = {
    request: {
        url: `${HOST}/contract/for-pdfsend`,
        data: {
            object_type: 'contract',
            dt_from: '2020-06-02T00:00:00',
            dt_to: '2020-06-03T00:00:00',
            service_id: '102',
            contract_eid: '1234',
            contract_type: 'GENERAL',
            firm_id: '4',
            payment_type: 3,
            pagination_ps: 10,
            pagination_pn: 1,
            is_faxed: true,
            is_email_enqueued: false,
            is_signed: true,
            is_sent_original: false,
            is_atypical_conditions: true,
            is_booked: false
        }
    },
    response: {
        totalCount: 2,
        items: [
            {
                contractEid: '1249096/20',
                objectType: 'ДС',
                isEmailSent: false,
                services: [],
                isFaxed: true,
                contractType: 'коммерческий',
                objectInfo: {
                    type: 'collateral',
                    id: 9285918
                },
                contractId: 4185908,
                isSentOriginal: false,
                isSigned: false,
                collateralNum: '01',
                isEmailEnqueued: '2020-06-03T00:00:00'
            },
            {
                contractEid: '1249280/20',
                objectType: 'ДС',
                isEmailSent: true,
                services: [],
                isFaxed: true,
                contractType: 'коммерческий',
                objectInfo: {
                    type: 'collateral',
                    id: 9286213
                },
                contractId: 4186143,
                isSentOriginal: false,
                isSigned: false,
                collateralNum: '01',
                isEmailEnqueued: '2020-06-03T00:00:00'
            }
        ]
    }
};

export const firmsEmails = {
    request: {
        url: `${HOST}/contract/pdf-email-addresses`
    },
    response: {
        firms: [
            {
                firm_id: 1,
                addresses: ['comission@yandex-team.ru', 'docs-project@yandex-team.ru']
            },
            {
                firm_id: 4,
                addresses: ['comission@yandex-team.ru']
            }
        ]
    }
};

export const sendEmailsSuccess = {
    request: {
        url: `${HOST}/contract/enqueue-print-form-emails`,
        isJSON: true,
        data: {
            email_to: 'asd@asd.asd',
            email_to_client: true,
            email_to_manager: true,
            email_from: 'comission@yandex-team.ru',
            email_subject: 'email subject',
            email_body: 'email body',
            objects: [
                {
                    object_id: 9285918,
                    object_type: 'collateral'
                }
            ]
        }
    },
    response: null
};

export const sendEmailsFailure = {
    request: {
        url: `${HOST}/contract/enqueue-print-form-emails`,
        isJSON: true,
        data: {
            email_to: 'asd@asd.asd',
            email_to_client: true,
            email_to_manager: true,
            email_from: 'comission@yandex-team.ru',
            email_subject: 'email subject',
            email_body: 'email body',
            objects: [
                {
                    object_id: 9285918,
                    object_type: 'collateral'
                }
            ]
        }
    },
    error: true
};
