/* tslint:disable:ter-max-len */

import {Table, PartnerLinkType} from 'app/types/consts';
import {Schema} from 'app/types/db/partners';
import {PartnerOptionBookinkLinkTarget} from 'app/v1/joi-schemas/partner';

interface Row {
    data: Schema['data'];
    partner_links?: Schema['partner_links'];
}

const rows: Row[] = [
    {
        // id: is serial and auto incremented
        data: {
            url: 'https://www.the-village.ru/',
            aref: '#thevillage',
            icon: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1357607/2a00000166b13f108389709fe5e968844425/%s'
            },
            image: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1357607/2a00000166b13f1f00c80c330f8c9adcc817/%s'
            },
            title: 'The Village',
            options: {
                bookingLinkTarget: {
                    value: PartnerOptionBookinkLinkTarget.BLANK
                }
            },
            linkType: PartnerLinkType.BOOKING,
            linkTitle: 'Забронировать столик',
            description: 'Ваш любимый сайт о городе: герои, события, еда и все самое важное'
        },
        partner_links: [
            {
                links: [
                    {
                        url: 'https://kudago.com/msk/list/besplatnaya-moskva?utm_source=yandex-maps&utm_medium=promoblockmsk',
                        image: {
                            urlTemplate:
                                'https://avatars.mds.yandex.net/get-discovery-int/1339925/2a0000016a0671081fa5d71aa68c88ceccc9/%s'
                        },
                        title: 'Бесплатная Москва: здесь всегда свободный вход'
                    }
                ],
                titleMask: 'Интересные места и события на KudaGo',
                geoRegionId: 213
            },
            {
                links: [
                    {
                        url: 'https://kudago.com/spb/list/besplatnyj-peterburg-mesta-gde-vsegda-svobodnyj-vh?utm_source=yandex-maps&utm_medium=promoblockspb',
                        image: {
                            urlTemplate:
                                'https://avatars.mds.yandex.net/get-discovery-int/1339925/2a0000016a06db3564e81b5f78b54e46c5b8/%s'
                        },
                        title: 'Бесплатный Петербург: здесь всегда свободный вход'
                    }
                ],
                titleMask: 'Интересные места и события на KudaGo',
                geoRegionId: 2
            }
        ]
    },
    {
        data: {
            url: 'https://t.me/privetdrive',
            icon: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/218162/2a000001675ad8fe07dcaf01036d8d4eced8/%s'
            },
            image: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1357607/2a000001675ad9098646853b73be9f34c676/%s'
            },
            title: 'Маша Цицюрская',
            options: {
                bookingLinkTarget: {
                    value: PartnerOptionBookinkLinkTarget.BLANK
                }
            },
            linkType: PartnerLinkType.BOOKING,
            linkTitle: 'Забронировать столик',
            description: 'Редактор, автор телеграм-канала про книги и фанатка вкусной еды'
        }
    }
];

const partners = {
    table: Table.PARTNERS,
    rows: rows.map((row) => ({
        ...row,
        data: JSON.stringify(row.data),
        partner_links: JSON.stringify(row.partner_links)
    }))
};

export {rows, partners};
