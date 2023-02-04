import {readFileSync} from 'fs';
import {join} from 'path';

export const defaultUid = 123;

export const defaultRequest = {
    lang: 'ru_RU',
    limit: 2,
    offset: 1,
    origin: 'test'
};

export const defaultResponse = {
    meta: {
        lang: 'ru_RU',
        offset: 1,
        limit: 2,
        total: 10
    },
    data: [
        {
            organization: {
                id: '123',
                title: '',
                address: '',
                uri: 'ymapsbm1://org?oid=123'
            },
            photoList: [
                {
                    id: 'bNiN-Hr57SCevvupbarVs2fNAMVKPxz6',
                    urlTemplate: 'https://avatars.mds.yandex.net/get-altay/4233/2a000001694f894f687d6214a933cc070725/%s',
                    createdTime: '2019-03-05T20:26:33.043Z',
                    moderation: {
                        status: 'IN_PROGRESS',
                        declineReason: ''
                    }
                },
                {
                    id: 'XWhwIwGNONGwgz12NYqxiCGcjiopjXyqf',
                    urlTemplate: 'https://avatars.mds.yandex.net/get-altay/4233/2a000001694f889b0ae699f737d90931c8fb/%s',
                    createdTime: '2019-03-05T20:25:46.911Z',
                    moderation: {
                        status: 'IN_PROGRESS',
                        declineReason: ''
                    }
                }
            ]
        },
        {
            organization: {
                id: '132002434175',
                title: 'Узбекская кухня',
                address: 'Россия, Краснодарский край, Темрюкский район, станица Голубицкая, Набережная улица',
                image: {
                    urlTemplate: 'https://avatars.mds.yandex.net/get-altay/903198/2a00000162b28eff47e2261b89789528a1b2/%s'
                },
                uri: 'ymapsbm1://org?oid=132002434175'
            },
            photoList: [
                {
                    id: '16774',
                    urlTemplate: 'https://avatars.mds.yandex.net/get-altay/4879/2a00000165c852bfd209609dfef19776a2ae/%s',
                    createdTime: '2018-09-11T11:09:51.598Z',
                    moderation: {
                        status: 'ACCEPTED',
                        declineReason: ''
                    }
                }
            ]
        }
    ]
};

export const ugcResponse = {
    Photos: [],
    OrgPhotos: [
        {
            Photos: [
                {
                    PhotoId: 'bNiN-Hr57SCevvupbarVs2fNAMVKPxz6',
                    UrlTemplate: 'https://avatars.mds.yandex.net/get-altay/4233/2a000001694f894f687d6214a933cc070725/{size}',
                    Moderation: {
                        Status: 'IN_PROGRESS',
                        DeclineReason: ''
                    },
                    OrgId: '123',
                    CreatedTime: '2019-03-05T20:26:33.043Z'
                },
                {
                    PhotoId: 'XWhwIwGNONGwgz12NYqxiCGcjiopjXyqf',
                    UrlTemplate: 'https://avatars.mds.yandex.net/get-altay/4233/2a000001694f889b0ae699f737d90931c8fb/{size}',
                    Moderation: {
                        Status: 'IN_PROGRESS',
                        DeclineReason: ''
                    },
                    OrgId: '123',
                    CreatedTime: '2019-03-05T20:25:46.911Z'
                }
            ],
            OrgPhotos: [],
            Count: 2
        },
        {
            Photos: [
                {
                    PhotoId: '16774',
                    UrlTemplate: 'https://avatars.mds.yandex.net/get-altay/4879/2a00000165c852bfd209609dfef19776a2ae/{size}',
                    Moderation: {
                        Status: 'ACCEPTED',
                        DeclineReason: ''
                    },
                    OrgId: '132002434175',
                    CreatedTime: '2018-09-11T11:09:51.598Z'
                }
            ],
            OrgPhotos: [],
            Count: 1
        }
    ],
    Count: 10
};

export const geoSearchResponse = readFileSync(
    join(
        __dirname,
        '../../../../../src/tests/integration/v1/data/geosearch.photo.protobuf'
    )
);
