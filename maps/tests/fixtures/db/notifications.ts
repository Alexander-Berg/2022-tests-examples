import {Branch, Locale, NotificationKind, Table} from 'app/types/consts';

const rows = [
    {
        // id: is serial and auto incremented
        uid: '9929f7bded701k2066gf4',
        branch: Branch.DRAFT,
        locale: Locale.RU,
        geo_region_ids: [213],
        kind: NotificationKind.DISCOVERY,
        types: [],
        start_date: '2019-10-20T21:00:00.000Z',
        end_date: '2019-10-21T20:59:59.999Z',
        content: {
            bannerImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/912415/2a0000016ded797e1c4df799c0e70aedc45f/%s'
            },
            buttonImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1357607/2a0000016ded797499677f86e43a800825fe/%s'
            },
            description: 'Что нового в городе'
        }
    },
    {
        uid: '01ccf7aa6047bk8h5a673',
        branch: Branch.DRAFT,
        locale: Locale.RU,
        geo_region_ids: [2],
        kind: NotificationKind.DISCOVERY,
        types: [],
        start_date: '2020-03-01T09:00:00.000Z',
        end_date: '2020-04-05T20:59:59.999Z',
        segments: ['segment-1'],
        content: {
            action: {
                url: 'yandexmaps://maps.yandex.ru/?mode=showcase'
            },
            bannerImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1339925/2a00000171160f287039d3df1dbfa7146d44/%s'
            },
            buttonImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1635274/2a00000171160f2031ce0b9458be8d9e5e89/%s'
            },
            description: 'Как заправиться, не выходя из машины,\nзаказать еду и найти лекарства ближе к дому'
        }
    },
    {
        uid: 'f2beead85f8c2k88irgt2',
        branch: Branch.DRAFT,
        locale: Locale.RU,
        geo_region_ids: [213],
        kind: NotificationKind.DISCOVERY,
        types: [],
        start_date: '2020-03-01T09:00:00.000Z',
        end_date: '2020-04-05T20:59:59.999Z',
        content: {
            action: {
                url: 'yandexmaps://maps.yandex.ru/?mode=showcase'
            },
            bannerImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/218162/2a00000171160bf5f0d0aa71b4a0ef44700e/%s'
            },
            buttonImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1339925/2a00000171160be84d332f72e0152d8c4149/%s'
            },
            description: 'Как заправиться, не выходя из машины,\nзаказать еду и найти лекарства ближе к дому'
        }
    },
    {
        uid: 'f2beead85f8c2k88irgt2',
        branch: Branch.PUBLIC,
        locale: Locale.RU,
        geo_region_ids: [213],
        draft_id: 3,
        kind: NotificationKind.DISCOVERY,
        types: [],
        segments: ['segment-1'],
        start_date: '2020-03-01T09:00:00.000Z',
        end_date: '2020-04-05T20:59:59.999Z',
        content: {
            action: {
                url: 'yandexmaps://maps.yandex.ru/?mode=showcase'
            },
            bannerImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/218162/2a00000171160bf5f0d0aa71b4a0ef44700e/%s'
            },
            buttonImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1339925/2a00000171160be84d332f72e0152d8c4149/%s'
            },
            description: 'Как заправиться, не выходя из машины,\nзаказать еду и найти лекарства ближе к дому'
        }
    },
    {
        uid: 'a1l0dfj23sb90asdl1qw8',
        branch: Branch.DRAFT,
        locale: Locale.RU,
        geo_region_ids: [213],
        kind: NotificationKind.DISCOVERY,
        types: [],
        start_date: '2020-03-01T09:00:00.000Z',
        end_date: '2020-04-05T20:59:59.999Z',
        content: {
            action: {
                url: 'yandexmaps://maps.yandex.ru/?mode=showcase'
            },
            bannerImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/218162/2a00000171160bf5f0d0aa71b4a0ef44700e/%s'
            },
            buttonImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1339925/2a00000171160be84d332f72e0152d8c4149/%s'
            },
            description: 'Как заправиться, не выходя из машины,\nзаказать еду и найти лекарства ближе к дому'
        }
    },
    {
        uid: 'a1l0dfj23sb90asdl1qw8',
        branch: Branch.PUBLIC,
        locale: Locale.RU,
        geo_region_ids: [213],
        draft_id: 5,
        kind: NotificationKind.DISCOVERY,
        types: [],
        start_date: '2020-03-01T09:00:00.000Z',
        end_date: '2020-04-05T20:59:59.999Z',
        content: {
            background: {
                type: 'image',
                value: 'https://avatars.mds.yandex.net/get-discovery-int/218162/2a00000171160bf5f0d0aa71b4a0ef44700e/%s'
            },
            action: {
                url: 'yandexmaps://maps.yandex.ru/?mode=showcase'
            },
            iconImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/218162/2a00000171160bf5f0d0aa71b4a0ef44700e/%s'
            },
            description: 'Как заправиться, не выходя из машины,\nзаказать еду и найти лекарства ближе к дому',
            textColor: '#000000',
            showClose: false
        }
    }
];

const notifications = {
    table: Table.NOTIFICATIONS,
    rows: rows.map((item) => ({
        ...item,
        content: JSON.stringify(item.content)
    }))
};
export {notifications};
