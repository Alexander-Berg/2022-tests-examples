import { HOST } from 'common/utils/test-utils/common';

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
            id: 102,
            name: 'Afisha.MoviePass'
        },
        {
            cc: 'bug_bounty',
            url_orders: null,
            in_contract: true,
            id: 207,
            name: 'Bug bounty'
        }
    ]
};

export const products1 = {
    request: [
        `${HOST}/product/list`,
        {
            service_id: 102,
            sort_key: 'ID',
            sort_order: 'DESC',
            pagination_pn: 1,
            pagination_ps: 10
        },
        false,
        false
    ],
    response: {
        version: {
            snout: '1.0.255',
            muzzle: 'UNKNOWN',
            butils: '2.156'
        },
        data: {
            items: [
                {
                    commission_type: {
                        id: 118,
                        name: 'Билеты в кино'
                    },
                    name: 'Авансовый платеж за билеты в кино',
                    service: {
                        id: 102,
                        name: 'Afisha.MoviePass'
                    },
                    fullname: 'Авансовый платеж за билеты в кино',
                    id: 509143,
                    show_in_shop: 0,
                    manual_discount: 0,
                    media_discount: {
                        id: 118,
                        name: 'Билеты в кино'
                    },
                    englishname: null,
                    product_group: {
                        id: 504022,
                        name: 'Билеты в кино'
                    },
                    dt: '2018-05-18T17:08:28',
                    unit: 'рубли',
                    activity_type: {
                        id: 112,
                        name: 'Билеты в кино'
                    }
                },
                {
                    commission_type: {
                        id: 0,
                        name: 'Нет'
                    },
                    name: 'Afisha.MoviePass',
                    service: {
                        id: 102,
                        name: 'Afisha.MoviePass'
                    },
                    fullname: 'Afisha.MoviePass',
                    id: 509068,
                    show_in_shop: 0,
                    manual_discount: 0,
                    media_discount: {
                        id: 0,
                        name: 'Нет'
                    },
                    englishname: null,
                    product_group: {
                        id: 509067,
                        name: 'afisha_moviepass'
                    },
                    dt: '2018-04-19T15:32:07',
                    unit: 'клик',
                    activity_type: {
                        id: null,
                        name: null
                    }
                }
            ],
            total_count: 2
        }
    }
};

export const products2 = {
    request: [
        `${HOST}/product/list`,
        {
            service_id: 102,
            product_name: 'Авансовый платеж',
            sort_key: 'ID',
            sort_order: 'DESC',
            pagination_pn: 1,
            pagination_ps: 10
        },
        false,
        false
    ],
    response: {
        version: {
            snout: '1.0.255',
            muzzle: 'UNKNOWN',
            butils: '2.156'
        },
        data: {
            items: [
                {
                    commission_type: {
                        id: 118,
                        name: 'Билеты в кино'
                    },
                    name: 'Авансовый платеж за билеты в кино',
                    service: {
                        id: 102,
                        name: 'Afisha.MoviePass'
                    },
                    fullname: 'Авансовый платеж за билеты в кино',
                    id: 509143,
                    show_in_shop: 0,
                    manual_discount: 0,
                    media_discount: {
                        id: 118,
                        name: 'Билеты в кино'
                    },
                    englishname: null,
                    product_group: {
                        id: 504022,
                        name: 'Билеты в кино'
                    },
                    dt: '2018-05-18T17:08:28',
                    unit: 'рубли',
                    activity_type: {
                        id: 112,
                        name: 'Билеты в кино'
                    }
                }
            ],
            total_count: 1
        }
    }
};

export const products3 = {
    request: [
        `${HOST}/product/list`,
        {
            service_id: 102,
            product_name: 'Авансовый платеж',
            product_id: 509143,
            sort_key: 'ID',
            sort_order: 'DESC',
            pagination_pn: 1,
            pagination_ps: 10
        },
        false,
        false
    ],
    response: {
        version: {
            snout: '1.0.255',
            muzzle: 'UNKNOWN',
            butils: '2.156'
        },
        data: {
            items: [
                {
                    commission_type: {
                        id: 118,
                        name: 'Билеты в кино'
                    },
                    name: 'Авансовый платеж за билеты в кино',
                    service: {
                        id: 102,
                        name: 'Afisha.MoviePass'
                    },
                    fullname: 'Авансовый платеж за билеты в кино',
                    id: 509143,
                    show_in_shop: 0,
                    manual_discount: 0,
                    media_discount: {
                        id: 118,
                        name: 'Билеты в кино'
                    },
                    englishname: null,
                    product_group: {
                        id: 504022,
                        name: 'Билеты в кино'
                    },
                    dt: '2018-05-18T17:08:28',
                    unit: 'рубли',
                    activity_type: {
                        id: 112,
                        name: 'Билеты в кино'
                    }
                }
            ],
            total_count: 1
        }
    }
};
