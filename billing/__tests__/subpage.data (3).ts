import { HOST } from 'common/utils/test-utils/common';

export const mocks = {
    clientWithoutPersons: {
        request: {
            url: `${HOST}/client`,
            data: {
                client_id: undefined
            }
        },
        response: {
            hasPersons: false
        }
    },
    clientWithPersons: {
        request: {
            url: `${HOST}/client`,
            data: {
                client_id: undefined
            }
        },
        response: {
            hasPersons: true
        }
    },
    persons: {
        request: {
            url: `${HOST}/client/person`,
            data: {
                is_partner: false,
                mode: 'DEFAULT',
                sort_key: 'NAME',
                pagination_pn: 1,
                pagination_ps: 50,
                show_totals: true
            }
        },
        response: {
            items: [
                {
                    kpp: null,
                    kzIn: null,
                    bik: null,
                    envelopeAddress: '',
                    deliveryType: 0,
                    id: 17672240,
                    phone: '+7 905 1234567',
                    inn: null,
                    postcode: null,
                    hidden: false,
                    personCategory: { ur: false },
                    type: 'ph',
                    email: 'balanceassessors@yandex.ru',
                    name: 'Плательщик 1'
                }
            ],
            totalСount: 2
        }
    },
    emptyPersons: {
        request: {
            url: `${HOST}/client/person`,
            data: {
                is_partner: false,
                mode: 'DEFAULT',
                sort_key: 'NAME',
                pagination_pn: 1,
                pagination_ps: 50,
                show_totals: true
            }
        },
        response: {
            items: [],
            totalСount: 0
        }
    },
    archivedPersons: {
        request: {
            url: `${HOST}/client/person`,
            data: {
                is_partner: false,
                mode: 'ARCHIVED',
                sort_key: 'NAME',
                pagination_pn: 1,
                pagination_ps: 50,
                show_totals: true
            }
        },
        response: {
            items: [
                {
                    kpp: null,
                    kzIn: null,
                    bik: null,
                    envelopeAddress: '',
                    deliveryType: 0,
                    id: 17672240,
                    phone: '+7 905 1234567',
                    inn: null,
                    postcode: null,
                    hidden: false,
                    personCategory: { ur: false },
                    type: 'ph',
                    email: 'balanceassessors@yandex.ru',
                    name: 'Плательщик 1'
                }
            ],
            total_count: 1
        }
    },
    nextPersons: {
        request: {
            url: `${HOST}/client/person`,
            data: {
                is_partner: false,
                mode: 'DEFAULT',
                sort_key: 'NAME',
                pagination_pn: 2,
                pagination_ps: 50,
                show_totals: true
            }
        },
        response: {
            items: [
                {
                    kpp: null,
                    kzIn: null,
                    bik: null,
                    envelopeAddress: '',
                    deliveryType: 0,
                    id: 17672241,
                    phone: '+7 905 1234567',
                    inn: null,
                    postcode: null,
                    hidden: false,
                    personCategory: { ur: false },
                    type: 'ph',
                    email: 'balanceassessors@yandex.ru',
                    name: 'Плательщик 1'
                }
            ],
            totalСount: 2
        }
    }
};
