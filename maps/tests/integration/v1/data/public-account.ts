import {
    UgcPublicAccountResponse,
    PublicAccountResponse,
    PublicAccountReviewsResponse,
    EditPublicAccountResponse
} from 'app/providers/public-account';

export const publicAccountResponseRaw: UgcPublicAccountResponse = {
    career: {
        professions: [
            {
                level: 1,
                id: 'UGC_GAMER'
            },
            {
                level: 3,
                id: 'UGC_CITY_EXPERT'
            }
        ]
    },
    pkUserOptedOut: false,
    pkAccount: {
        name: 'name',
        pic: '39410/Wskd6HAAvqbVxHgyR83fhEQUsmY-1'
    },
    view: {
        count: 1,
        views: [
            {
                id: 'wFdU1vbi615_gaiYrThml7EDdjzhoezm',
                time: 1566558672268,
                text: 'VIEW_TEXT',
                reactions: {
                    dislikesCount: 2,
                    likesCount: 5
                },
                rating: {
                    max: 5,
                    val: 4
                },
                object: {
                    id: 'sprav/123123123',
                    type: 'Org',
                    title: 'Простые вещи',
                    subtitle: 'Москва, ул. Плющиха, 2',
                    thumb: 'https://avatars.mds.yandex.net/get-altay/492546/2a0000115dcbda4690691cabab025651c635/%s'
                }
            }
        ]
    }
};

export const publicAccountReviewsResponse: {
    enable: PublicAccountReviewsResponse,
    disable: PublicAccountReviewsResponse
} = {
    enable: {
        meta: {
            limit: 10,
            offset: 0,
            total: 1,
            lang: 'ru_RU'
        },
        data: [
            {
                organization: {
                    id: '123123123',
                    title: 'Простые вещи',
                    address: 'Москва, ул. Плющиха, 2',
                    image: {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/492546/2a0000115dcbda4690691cabab025651c635/%s'
                    },
                    uri: 'ymapsbm1://org?oid=123123123'
                },
                review: {
                    id: 'wFdU1vbi615_gaiYrThml7EDdjzhoezm',
                    rating: 4,
                    message: 'VIEW_TEXT',
                    updateTime: '2019-08-23T11:11:12.268Z',
                    likesCount: 5,
                    dislikesCount: 2,
                    viewsCount: 7,
                    photos: []
                }
            }
        ]
    },
    disable: {
        meta: {
            limit: 10,
            offset: 0,
            total: 0,
            lang: 'ru_RU'
        },
        data: []
    }
};

export const publicAccountResponse: {
    enable: PublicAccountResponse,
    disable: PublicAccountResponse
} = {
    enable: {
        enable: true,
        account: {
            name:  'name',
            pic: `https://avatars.mds.yandex.net/get-yapic/39410/Wskd6HAAvqbVxHgyR83fhEQUsmY-1/islands-300`
        },
        professions: {
            cityExpertLevel: 3
        }
    },

    disable: {
        enable: false
    }
};

export const editAccessData: {answer: EditPublicAccountResponse} = {
    answer: {
        success: true
    }
};
