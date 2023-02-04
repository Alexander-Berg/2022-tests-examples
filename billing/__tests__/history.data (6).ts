import { HOST } from 'common/utils/test-utils/common';

export const variants = {
    РСЯ: {
        historyQS: {
            client_id: '',
            contract_class: 'PARTNERS',
            contract_eid: 'YAN-119717-01/20',
            contract_eid_like: '0',
            contract_type: '9',
            dt_from: '2020-01-01T00:00:00',
            dt_to: '2020-01-31T00:00:00',
            date_type: '1',
            doc_set: '',
            firm: '7',
            is_offer: '',
            manager_code: '',
            payment_type: '1',
            person_id: '',
            platform_type: '',
            pn: '1',
            ps: '10',
            service_id: '',
            sf: 'start_dt',
            so: '1',
            tag_id: '',
            tag_name: ''
        },
        historyState: {
            billInterval: '1',
            contractClass: 'PARTNERS',
            contractEid: 'YAN-119717-01/20',
            contractEidLike: false,
            contractType: '9',
            docSet: 'ALL',
            dtFrom: '2020-01-01T00:00:00',
            dtTo: '2020-01-31T00:00:00',
            dtType: '1',
            firmId: '7',
            offer: 'ALL'
        },
        partnerContracts: {
            request: {
                data: {
                    bill_intervals: '1',
                    contract_class: 'PARTNERS',
                    contract_eid: 'YAN-119717-01/20',
                    contract_eid_like: false,
                    contract_type: '9',
                    date_type: 'STARTED',
                    dt_from: '2020-01-01T00:00:00',
                    dt_to: '2020-01-31T00:00:00',
                    firm_id: '7',
                    pagination_pn: 1,
                    pagination_ps: 10,
                    sort_key: 'START_DT',
                    sort_order: 'ASC'
                },
                url: `${HOST}/contract/partner-contracts`
            },
            response: {
                items: [],
                total_row_count: 0
            }
        }
    },
    Дистрибуция: {
        historyQS: {
            client_id: '',
            contract_class: 'DISTRIBUTION',
            contract_eid: 'ДС-29207-01/20',
            contract_eid_like: '0',
            contract_type: '',
            dt_from: '2020-01-01T00:00:00',
            dt_to: '2020-01-31T00:00:00',
            date_type: '1',
            doc_set: '',
            firm: '1',
            is_offer: '',
            manager_code: '',
            payment_type: '',
            person_id: '',
            platform_type: '2',
            pn: '1',
            ps: '10',
            service_id: '',
            sf: 'start_dt',
            so: '1',
            tag_id: '',
            tag_name: ''
        },
        historyState: {
            billInterval: 'ALL',
            contractClass: 'DISTRIBUTION',
            contractEid: 'ДС-29207-01/20',
            contractEidLike: false,
            contractType: null,
            docSet: null,
            dtFrom: '2020-01-01T00:00:00',
            dtTo: '2020-01-31T00:00:00',
            dtType: '1',
            firmId: '1',
            offer: 'ALL'
        },
        partnerContracts: {
            request: {
                data: {
                    contract_class: 'DISTRIBUTION',
                    contract_eid: 'ДС-29207-01/20',
                    contract_eid_like: false,
                    date_type: 'STARTED',
                    dt_from: '2020-01-01T00:00:00',
                    dt_to: '2020-01-31T00:00:00',
                    firm_id: '1',
                    pagination_pn: 1,
                    pagination_ps: 10,
                    platform_type: '2',
                    sort_key: 'START_DT',
                    sort_order: 'ASC'
                },
                url: `${HOST}/contract/partner-contracts`
            },
            response: {
                items: [],
                total_row_count: 0
            }
        }
    },
    Справочник: {
        historyQS: {
            contract_class: 'GEOCONTEXT',
            contract_eid: '243-12\\10',
            contract_eid_like: '0',
            contract_type: '',
            date_type: '1',
            doc_set: '',
            firm: '',
            is_offer: '',
            payment_type: '',
            platform_type: '',
            pn: '1',
            ps: '10',
            service_id: '',
            sf: 'start_dt',
            so: '1',
            tag_id: '',
            tag_name: ''
        },
        historyState: {
            billInterval: 'ALL',
            contractClass: 'GEOCONTEXT',
            contractEid: '243-12\\10',
            contractEidLike: false,
            contractType: null,
            docSet: null,
            dtFrom: null,
            dtTo: null,
            dtType: '1',
            firmId: 'ALL',
            offer: 'ALL'
        },
        partnerContracts: {
            request: {
                data: {
                    contract_class: 'GEOCONTEXT',
                    contract_eid: '243-12\\10',
                    contract_eid_like: false,
                    date_type: 'STARTED',
                    pagination_pn: 1,
                    pagination_ps: 10,
                    sort_key: 'START_DT',
                    sort_order: 'ASC'
                },
                url: `${HOST}/contract/partner-contracts`
            },
            response: {
                items: [],
                total_row_count: 0
            }
        }
    },
    Афиша: {
        historyQS: {
            contract_class: 'AFISHA',
            contract_eid: '121',
            contract_eid_like: '0',
            contract_type: '',
            date_type: '1',
            doc_set: '',
            firm: '',
            is_offer: '',
            payment_type: '',
            platform_type: '',
            pn: '1',
            ps: '10',
            service_id: '',
            sf: 'start_dt',
            so: '1',
            tag_id: '',
            tag_name: ''
        },
        historyState: {
            billInterval: 'ALL',
            contractClass: 'AFISHA',
            contractEid: '121',
            contractEidLike: false,
            contractType: null,
            docSet: 'ALL',
            dtFrom: null,
            dtTo: null,
            dtType: '1',
            firmId: 'ALL',
            offer: 'ALL'
        },
        partnerContracts: {
            request: {
                data: {
                    contract_class: 'AFISHA',
                    contract_eid: '121',
                    contract_eid_like: false,
                    date_type: 'STARTED',
                    pagination_pn: 1,
                    pagination_ps: 10,
                    sort_key: 'START_DT',
                    sort_order: 'ASC'
                },
                url: `${HOST}/contract/partner-contracts`
            },
            response: {
                items: [],
                total_row_count: 0
            }
        }
    },
    Афиша2: {
        historyQS: {
            client_id: '',
            contract_class: 'AFISHA',
            contract_eid: '1234',
            contract_eid_like: '1',
            contract_type: '',
            dt_from: '2020-09-04T00:00:00',
            dt_to: '2020-09-04T00:00:00',
            date_type: '2',
            doc_set: '1',
            firm: '1',
            is_offer: '1',
            manager_code: '',
            payment_type: '1',
            person_id: '',
            platform_type: '',
            pn: '1',
            ps: '10',
            service_id: '',
            sf: 'start_date',
            so: '1',
            tag_id: '',
            tag_name: ''
        },
        historyState: {
            contractClass: 'AFISHA',
            firmId: '1',
            dtType: '2',
            dtFrom: '2020-09-04T00:00:00',
            dtTo: '2020-09-04T00:00:00',
            contractEid: '1234',
            contractEidLike: true,
            contractType: null,
            offer: '1',
            billInterval: '1',
            docSet: '1'
        },
        partnerContracts: {
            request: {
                data: {
                    bill_intervals: '1',
                    contract_class: 'AFISHA',
                    contract_eid: '1234',
                    contract_eid_like: true,
                    date_type: 'FINISHED',
                    doc_set: '1',
                    dt_from: '2020-09-04T00:00:00',
                    dt_to: '2020-09-04T00:00:00',
                    firm_id: '1',
                    is_offer: true,
                    pagination_pn: 1,
                    pagination_ps: 10,
                    sort_key: 'START_DT',
                    sort_order: 'ASC'
                },
                url: `${HOST}/contract/partner-contracts`
            },
            response: {
                items: [],
                total_row_count: 0
            }
        }
    },
    'Приоритетная сделка': {
        historyQS: {
            contract_class: 'PREFERRED_DEAL',
            contract_eid: '3f13b6802c554086a69aa5c60ab2018f',
            contract_eid_like: '0',
            contract_type: '',
            date_type: '1',
            doc_set: '',
            firm: '',
            is_offer: '',
            payment_type: '',
            platform_type: '',
            pn: '1',
            ps: '10',
            service_id: '',
            sf: 'start_dt',
            so: '1',
            tag_id: '',
            tag_name: ''
        },
        historyState: {
            billInterval: 'ALL',
            contractClass: 'PREFERRED_DEAL',
            contractEid: '3f13b6802c554086a69aa5c60ab2018f',
            contractEidLike: false,
            contractType: null,
            docSet: 'ALL',
            dtFrom: null,
            dtTo: null,
            dtType: '1',
            firmId: 'ALL',
            offer: 'ALL'
        },
        partnerContracts: {
            request: {
                data: {
                    contract_class: 'PREFERRED_DEAL',
                    contract_eid: '3f13b6802c554086a69aa5c60ab2018f',
                    contract_eid_like: false,
                    date_type: 'STARTED',
                    pagination_pn: 1,
                    pagination_ps: 10,
                    sort_key: 'START_DT',
                    sort_order: 'ASC'
                },
                url: `${HOST}/contract/partner-contracts`
            },
            response: {
                items: [],
                total_row_count: 0
            }
        }
    },
    Расходный: {
        historyQS: {
            client_id: '',
            contract_class: 'SPENDABLE',
            contract_eid: '14638',
            contract_eid_like: '1',
            contract_type: '',
            dt_from: '2020-01-01T00:00:00',
            dt_to: '2020-01-31T00:00:00',
            date_type: '2',
            doc_set: '',
            firm: '13',
            is_offer: '0',
            manager_code: '',
            payment_type: '1',
            person_id: '',
            platform_type: '',
            pn: '1',
            ps: '10',
            service_id: '135',
            sf: 'start_dt',
            so: '1',
            tag_id: '',
            tag_name: ''
        },
        historyState: {
            billInterval: '1',
            contractClass: 'SPENDABLE',
            contractEid: '14638',
            contractEidLike: true,
            contractType: null,
            docSet: null,
            dtFrom: '2020-01-01T00:00:00',
            dtTo: '2020-01-31T00:00:00',
            dtType: '2',
            firmId: '13',
            offer: '0'
        },
        partnerContracts: {
            request: {
                data: {
                    bill_intervals: '1',
                    contract_class: 'SPENDABLE',
                    contract_eid: '14638',
                    contract_eid_like: true,
                    date_type: 'FINISHED',
                    dt_from: '2020-01-01T00:00:00',
                    dt_to: '2020-01-31T00:00:00',
                    firm_id: '13',
                    is_offer: false,
                    pagination_pn: 1,
                    pagination_ps: 10,
                    service_id: '135',
                    sort_key: 'START_DT',
                    sort_order: 'ASC'
                },
                url: `${HOST}/contract/partner-contracts`
            },
            response: {
                items: [],
                total_row_count: 0
            }
        }
    },
    Эквайринг: {
        historyQS: {
            contract_class: 'SPENDABLE',
            contract_eid: '',
            contract_eid_like: '0',
            contract_type: '',
            dt_from: '2020-02-01T00:00:00',
            dt_to: '2020-02-29T00:00:00',
            date_type: '2',
            doc_set: '',
            firm: '',
            is_offer: '',
            payment_type: '',
            platform_type: '',
            pn: '1',
            ps: '10',
            service_id: '',
            sf: 'start_dt',
            so: '1',
            tag_id: '',
            tag_name: ''
        },
        historyState: {
            billInterval: 'ALL',
            contractClass: 'SPENDABLE',
            contractEid: '',
            contractEidLike: false,
            contractType: null,
            docSet: null,
            dtFrom: '2020-02-01T00:00:00',
            dtTo: '2020-02-29T00:00:00',
            dtType: '2',
            firmId: 'ALL',
            offer: 'ALL'
        },
        partnerContracts: {
            request: {
                data: {
                    contract_class: 'SPENDABLE',
                    contract_eid_like: false,
                    date_type: 'FINISHED',
                    dt_from: '2020-02-01T00:00:00',
                    dt_to: '2020-02-29T00:00:00',
                    pagination_pn: 1,
                    pagination_ps: 10,
                    sort_key: 'START_DT',
                    sort_order: 'ASC'
                },
                url: `${HOST}/contract/partner-contracts`
            },
            response: {
                items: [],
                total_row_count: 0
            }
        }
    }
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
            dt_from: '2020-09-04T00:00:00',
            dt_to: '2020-09-04T00:00:00',
            firm_id: '1',
            is_offer: true,
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
                sent_dt: null,
                contract_class: 'PARTNERS',
                currency: 643,
                contract_type: 5,
                reward_type: 1,
                partner_pct: 43,
                client_name: 'balance_test 2020-09-04 01:30:02.303454',
                manager_code: 20160,
                is_signed: '2020-09-04T00:00:00',
                client_login: null,
                person_id: 12491308,
                firm: 1,
                finish_dt: '2020-09-11T00:00:00',
                test_mode: null,
                person_name: 'Юр. лицо РСЯRDWF Владимиров',
                contract_eid: 'РСЯ-144741-09/20',
                is_booked_dt: null,
                doc_set: null,
                tag_id: null,
                manager_name: 'Нигай Елена Сергеевна',
                is_faxed: null,
                bill_intervals: 1,
                client_id: 1339103779,
                services: null,
                contract_id: 2148180,
                is_offer: null,
                platform_type: null,
                tag_name: null,
                service_id: null,
                start_dt: '2020-09-04T00:00:00',
                is_cancelled: '2099-01-01T00:00:00'
            },
            {
                sent_dt: null,
                contract_class: 'PARTNERS',
                currency: 643,
                contract_type: 1,
                reward_type: 1,
                partner_pct: 45,
                client_name: 'createdAgency_XSCTH',
                manager_code: 20160,
                is_signed: '2020-09-04T00:00:00',
                client_login: null,
                person_id: 12493573,
                firm: 1,
                finish_dt: '2021-09-04T00:00:00',
                test_mode: null,
                person_name: 'Юр. лицо РСЯEzmv',
                contract_eid: 'РС-144757-09/20',
                is_booked_dt: null,
                doc_set: 4,
                tag_id: null,
                manager_name: 'Нигай Елена Сергеевна',
                is_faxed: '2020-09-04T00:00:00',
                bill_intervals: 1,
                client_id: 1339108271,
                services: null,
                contract_id: 2149595,
                is_offer: null,
                platform_type: null,
                tag_name: null,
                service_id: null,
                start_dt: '2020-09-04T00:00:00',
                is_cancelled: '2020-09-04T00:00:00'
            }
        ],
        total_row_count: 2
    }
};
