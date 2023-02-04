/* tslint:disable:ter-max-len */
/* tslint:disable:no-irregular-whitespace */

import {Table, Locale, Branch, DisplayMode, StoryScreenType, TypeLink, StoryButtonType} from 'app/types/consts';
import {Schema as StoryContentSchema} from 'app/types/db/stories-content';

type Schema = Omit<StoryContentSchema, 'id'>;

const draftedRows: Schema[] = [
    {
        // id: is auto incremented
        story_id: '671c0cdf-1283-4e15-9f68-bad87c174070',
        locale: Locale.RU,
        branch: Branch.DRAFT,
        info: {
            meta: {
                userLogin: 'SOME_USER'
            },
            title: 'Как узнать, какие кафе привезут еду на дом',
            properties: {
                settings: {
                    displayMode: DisplayMode.REGULAR
                }
            },
            previewImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1532686/2a000001711227cefdc4c459f824d944802f/%s'
            }
        },
        screens: [
            {
                id: '53324d55451c5k4b2gu12',
                type: StoryScreenType.PHOTO,
                buttons: [],
                content: [
                    {
                        width: 1080,
                        height: 1920,
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1635274/2a0000016f183e3e58ae15977ef873c597a0/%s'
                    },
                    {
                        width: 1124,
                        height: 2436,
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016f183e47f62ed06c52cc14f13140/%s'
                    }
                ]
            },
            {
                id: '811c2472d5f25k87fx4ji',
                type: StoryScreenType.VIDEO,
                buttons: [],
                content: [
                    {
                        width: 1080,
                        height: 1920,
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/912415/2a0000017112282e6baf42ad5562417ab96a/%s'
                    },
                    {
                        width: 1124,
                        height: 2436,
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a00000171122844d7dc6579874866b7bbb4/%s'
                    }
                ]
            },
            {
                id: '15d0681582474k87fxi9a',
                type: StoryScreenType.VIDEO,
                buttons: [
                    {
                        url: 'доставка еды',
                        tags: [],
                        type: StoryButtonType.OPEN_URL,
                        title: 'Посмотреть кафе с доставкой рядом ',
                        typeLink: TypeLink.SEARCH_TEXT,
                        titleColor: 'FFFFFF',
                        backgroundColor: '4CA6FF'
                    }
                ],
                content: [
                    {
                        width: 1124,
                        height: 2436,
                        videoId: '11a1e679-e3be-4aad-a3bb-591eada9cc44'
                    },
                    {
                        width: 1080,
                        height: 1920,
                        videoId: 'c2bc6a4d-d6bd-4f37-8a08-abb7a63a91e1'
                    }
                ]
            }
        ],
        start_date: '2020-03-25 00:00:00.000000',
        weight: 1
    },
    {
        // Contains unresolved video, it is necessary for tests
        story_id: '6f37895b-ecc9-4347-aa27-d08a7b6c9c8d',
        locale: Locale.RU,
        branch: Branch.DRAFT,
        info: {
            meta: {
                userLogin: 'SOME_USER'
            },
            title: 'Как заправиться, не выходя из машины',
            properties: {
                settings: {
                    displayMode: DisplayMode.REGULAR
                }
            },
            previewImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1532686/2a0000017111fe52c7349125815e24453d76/%s'
            }
        },
        screens: [
            {
                id: 'e286976d7bcf4k87eam8p',
                type: StoryScreenType.PHOTO,
                buttons: [],
                content: [
                    {
                        width: 1080,
                        height: 1920,
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/912415/2a0000017111fe7c49b2ee045c254b73a7fd/%s'
                    },
                    {
                        width: 1124,
                        height: 2436,
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000017111fe92f9a1a13526fd18758f88/%s'
                    }
                ]
            },
            {
                id: '5cab5fb332ab6k87eaxut',
                type: StoryScreenType.VIDEO,
                buttons: [
                    {
                        url: 'yandexmaps://maps.yandex.ru/?text=%28provider%3Agas_stations%29%20%28category_id%3A184105274%20%7C%20category_id%3A184105272%29&display-text=%D0%90%D0%97%D0%A1%20%D1%81%20%D0%BE%D0%BF%D0%BB%D0%B0%D1%82%D0%BE%D0%B9%20%D0%B2%20%D0%9A%D0%B0%D1%80%D1%82%D0%B0%D1%85',
                        tags: [],
                        type: StoryButtonType.OPEN_URL,
                        title: 'Посмотреть ближайшие заправки ',
                        typeLink: TypeLink.EXTERNAL_LINK,
                        titleColor: 'FFFFFF',
                        backgroundColor: '4CA6FF'
                    }
                ],
                content: [
                    {
                        width: 1124,
                        height: 2436,
                        videoId: 'ad5937ae-52ef-46bf-b688-936faa13598e'
                    },
                    {
                        width: 1080,
                        height: 1920,
                        videoId: '25290259-8653-497a-8de2-5bf77bae8ccc'
                    }
                ]
            }
        ],
        start_date: '2020-03-25 00:00:00.000000',
        weight: 2
    },
    {
        story_id: 'ba6a1ef9-9454-45c4-aa58-96488edc3599',
        locale: Locale.RU,
        branch: Branch.DRAFT,
        info: {
            meta: {
                userLogin: 'SOME_USER'
            },
            title: 'Как оставаться на связи в самоизоляции',
            properties: {
                settings: {
                    displayMode: DisplayMode.REGULAR
                }
            },
            previewImage: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1635274/2a00000171354a0587a15a059f4fc7f68320/%s'
            },
            previewImageContent: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1635274/2a00000171354a11033584b598db28e204d6/%s'
            }
        },
        screens: [
            {
                id: 'f9abf7902ebcck8h6us8p',
                type: StoryScreenType.PHOTO,
                buttons: [
                    {
                        url: 'https://mgts.ru/leader',
                        tags: [],
                        type: StoryButtonType.OPEN_URL,
                        title: 'Подключиться',
                        typeLink: TypeLink.EXTERNAL_LINK,
                        titleColor: 'FFFFFF',
                        backgroundColor: '4CA6FF'
                    }
                ],
                content: [
                    {
                        width: 1080,
                        height: 1920,
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/912415/2a00000171354a4078b16c3c0210f6be66f6/%s'
                    }
                ]
            },
            {
                id: 'c1d49c547078bk8h6vglu',
                type: StoryScreenType.PHOTO,
                buttons: [
                    {
                        url: 'https://mgts.ru/leader',
                        tags: [],
                        type: StoryButtonType.OPEN_URL,
                        title: 'Подключиться',
                        typeLink: TypeLink.EXTERNAL_LINK,
                        titleColor: 'FFFFFF',
                        backgroundColor: '4CA6FF'
                    }
                ],
                content: [
                    {
                        width: 1080,
                        height: 1920,
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/912415/2a00000171354ab67394adbf352f674f65a1/%s'
                    }
                ]
            },
            {
                id: '15d0681582474k87fxi9a',
                type: StoryScreenType.VIDEO,
                buttons: [
                    {
                        url: 'доставка еды',
                        tags: [],
                        type: StoryButtonType.OPEN_URL,
                        title: 'Посмотреть кафе с доставкой рядом ',
                        typeLink: TypeLink.SEARCH_TEXT,
                        titleColor: 'FFFFFF',
                        backgroundColor: '4CA6FF'
                    }
                ],
                content: [
                    {
                        width: 1124,
                        height: 2436,
                        videoId: '11a1e679-e3be-4aad-a3bb-591eada9cc44'
                    },
                    {
                        width: 1080,
                        height: 1920,
                        videoId: 'c2bc6a4d-d6bd-4f37-8a08-abb7a63a91e1'
                    }
                ]
            }
        ],
        start_date: '2019-07-29 00:00:00.000000',
        weight: 0
    }
];

// Create published stories content by draft
const rows: Schema[] = [...draftedRows];
draftedRows.forEach((row) =>
    rows.push({
        ...row,
        branch: Branch.PUBLIC
    })
);

const storiesContent = {
    table: Table.STORIES_CONTENT,
    rows: rows.map((row) => ({
        ...row,
        info: JSON.stringify(row.info),
        screens: JSON.stringify(row.screens)
    }))
};

export {storiesContent};
