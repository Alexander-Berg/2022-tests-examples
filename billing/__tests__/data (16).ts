export const product = {
    version: {
        snout: '1.0.258',
        muzzle: 'UNKNOWN',
        butils: '2.158'
    },
    data: {
        commission_type: {
            id: 118,
            name: 'Билеты в кино'
        },
        adv_kind: {
            id: null,
            name: null
        },
        id: 509143,
        unit: 'рубли',
        activ_dt: '2018-05-01T00:00:00',
        service: {
            id: 617,
            name: 'Afisha.MoviePass'
        },
        hidden: false,
        product_names: [
            {
                lang: 'ru',
                fullname: 'Авансовый платеж за билеты в кино',
                name: 'Авансовый платеж за билеты в кино'
            }
        ],
        comments: 'https://st.yandex-team.ru/DOCUMENT-25536 Васильева',
        show_in_shop: 0,
        dt: '2018-05-18T17:08:28',
        product_group: {
            id: 504022,
            name: 'Билеты в кино'
        },
        firm: {
            id: 121,
            title: 'ООО «Яндекс.Медиасервисы»'
        },
        prices: [
            {
                price: '1.000000',
                iso_currency: 'RUB',
                dt: '2018-05-01T00:00:00',
                tax_policy_pct: {
                    dt: '2004-01-01T00:00:00',
                    nsp_pct: '0.00',
                    id: 1,
                    nds_pct: '18.00',
                    tax_policy: {
                        id: 1,
                        name: 'Стандартный НДС'
                    }
                },
                id: 32513
            },
            {
                price: '1.000000',
                iso_currency: 'RUB',
                dt: '2019-01-01T00:00:00',
                tax_policy_pct: {
                    dt: '2019-01-01T00:00:00',
                    nsp_pct: '0.00',
                    id: 281,
                    nds_pct: '20.00',
                    tax_policy: {
                        id: 1,
                        name: 'Стандартный НДС'
                    }
                },
                id: 38329
            }
        ],
        englishname: null,
        name: 'Авансовый платеж за билеты в кино',
        service_code: {
            code: null,
            descr: null
        },
        taxes: [
            {
                firm: {
                    id: 1,
                    title: 'ООО «Яндекс»'
                },
                dt: '2018-05-01T00:00:00',
                iso_currency: 'RUB',
                id: 34098,
                tax_policy: {
                    id: 1,
                    name: 'Стандартный НДС'
                }
            },
            {
                firm: {
                    id: 121,
                    title: 'ООО «Яндекс.Медиасервисы»'
                },
                dt: '2018-07-01T00:00:00',
                iso_currency: 'RUB',
                id: 34804,
                tax_policy: {
                    id: 1,
                    name: 'Стандартный НДС'
                }
            }
        ],
        manual_discount: 0,
        media_discount: {
            id: 118,
            name: 'Билеты в кино'
        },
        fullname: 'Авансовый платеж за билеты в кино',
        activity_type: {
            id: 112,
            name: 'Билеты в кино'
        },
        markups: [],
        season_coefficient: []
    }
};

export const productFull = {
    ...product,
    data: {
        ...product.data,
        season_coefficient: [
            {
                finish_dt: '2012-02-01T00:00:00',
                coeff: '60.00',
                dt: '2012-01-01T00:00:00',
                id: 189
            },
            {
                finish_dt: '2012-03-01T00:00:00',
                coeff: '70.00',
                dt: '2012-02-01T00:00:00',
                id: 189
            },
            {
                finish_dt: '2013-01-01T00:00:00',
                coeff: '140.00',
                dt: '2012-09-01T00:00:00',
                id: 1900
            }
        ],
        markups: [
            {
                code: 'geo_msk',
                description: 'нацеливание на Москву и МО',
                pct: '20.00',
                id: 16063
            },
            {
                code: 'geo_spb',
                description: 'нацеливание на Санкт-Петербург и ЛО',
                pct: '10.00',
                id: 16067
            },
            {
                code: 'geo_msk_spb',
                description: 'нацеливание на Москву и МО, Санкт-Петербург и ЛО',
                pct: '20.00',
                id: 16071
            },
            {
                code: 'geo_msk_rgn',
                description: 'прореживание Санкт-Петербурга и ЛО',
                pct: '20.00',
                id: 16075
            },
            {
                code: 'geo_spb_rgn',
                description: 'прореживание Москвы и МО',
                pct: '10.00',
                id: 16079
            },
            {
                code: 'geo_rgn-',
                description: 'прореживание Москвы и МО, Санкт-Петербурга и ЛО',
                pct: '-20.00',
                id: 16083
            },
            {
                code: 'geo_rgn+',
                description: 'нацеливание на регионы России',
                pct: '20.00',
                id: 16087
            },
            {
                code: 'geo_rgn',
                description: 'Отдельные регионы',
                pct: '-20.00',
                id: 16091
            },
            {
                code: 'geo_ru',
                description: 'Вся Россия',
                pct: '0.00',
                id: 16095
            }
        ]
    }
};
