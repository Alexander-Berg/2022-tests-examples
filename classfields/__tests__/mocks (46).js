import merge from 'lodash/merge';

const defaultState = {
    config: {
        origin: ''
    },
    geo: {
        locative: 'в Иркутской области',
        zoom: 4,
        latitude: 55.65813,
        longitude: 37.752975
    },
    profiles: {
        redirectPhones: {}
    },
    search: {
        searchQuery: {
            type: 'SELL',
            category: 'APARTMENT',
            rgid: 225,
            uid: '4044179125'
        }
    },
    page: {
        params: {}
    },
    map: {
        offers: {
            points: [
                {
                    id: '5202946943251421128',
                    geoId: '55.65813,37.752975',
                    lat: 55.65813,
                    lon: 37.752975,
                    price: 5520000,
                    isNewbuilding: false,
                    favoriteOfferIds: [],
                    count: 1,
                    type: 'offer'
                },
                {
                    id: '4729343926261745876',
                    geoId: '55.657253,37.754017',
                    lat: 55.657253,
                    lon: 37.754017,
                    price: 1670000,
                    isNewbuilding: false,
                    favoriteOfferIds: [],
                    count: 2,
                    type: 'offer'
                }
            ]
        }
    },
    cards: {
        profile: {
            name: 'Евгений Горбачев',
            logo: {
                // eslint-disable-next-line max-len
                appSnippetSmall: '//avatars.mdst.yandex.net/get-realty/2935/add.15912706571766b0811e892/app_snippet_small',
                appMiddle: '//avatars.mdst.yandex.net/get-realty/2935/add.15912706571766b0811e892/app_middle'
            },
            foundationDate: '2020-01-21T21:00:00Z',
            address: {
                unifiedAddress: 'Санкт-Петербург, Кушелевская дорога, 3к2',
                point: {
                    latitude: 59.98799,
                    longitude: 59.98799
                }
            },
            workSchedule: [
                {
                    day: 'FRIDAY',
                    minutesFrom: 480,
                    minutesTo: 1320
                },
                {
                    day: 'MONDAY',
                    minutesFrom: 480,
                    minutesTo: 1320
                },
                {
                    day: 'THURSDAY',
                    minutesFrom: 480,
                    minutesTo: 1320
                },
                {
                    day: 'TUESDAY',
                    minutesFrom: 480,
                    minutesTo: 1320
                },
                {
                    day: 'WEDNESDAY',
                    minutesFrom: 480,
                    minutesTo: 1320
                }
            ],
            // eslint-disable-next-line max-len
            description: 'Но тщательные исследования конкурентов неоднозначны и будут в равной степени предоставлены сами себе. В своём стремлении повысить качество жизни, они забывают, что начало повседневной работы по формированию позиции не оставляет шанса для системы обучения кадров, соответствующей насущным потребностям. Каждый из нас понимает очевидную вещь: высококачественный прототип будущего проекта представляет собой интересный эксперимент проверки распределения внутренних резервов и ресурсов.',
            creationDate: '2020-01-21T21:00:00Z',
            userType: 'AGENT',
            profileUid: '4044179125',
            offers: {
                total: {
                    count: 2
                },
                subjectFederation: {
                    count: 1,
                    rgid: 741964
                }
            }
        }
    }
};

const getState = (overridesProfile, overrideState) => {
    const state = {
        ...defaultState,
        cards: {
            profile: merge({}, defaultState.cards.profile, overridesProfile)
        }
    };

    return merge({}, state, overrideState);
};

export default {
    defaultAgent: getState(),
    defaultAgency: getState({ name: 'Миэль', userType: 'AGENCY' }),
    withoutOffersInUserRegion: getState({ offers: { subjectFederation: { count: 0 } } }),
    withoutOffers: getState({ offers: { subjectFederation: { count: 0 }, total: { count: 0 } } }),
    userHasSameGeoAsAProfile: getState({ offers: { subjectFederation: { count: 2 } } }),
    // eslint-disable-next-line max-len
    withRedirectPhones: getState({}, { profiles: { redirectPhones: { 4044179125: { phones: [ '+79991234567' ], status: 'SUCCESS' } } } }),
    // eslint-disable-next-line max-len
    withTwoRedirectPhones: getState({}, { profiles: { redirectPhones: { 4044179125: { phones: [ '+79991234567', '+79994444567' ], status: 'SUCCESS' } } } })
};

