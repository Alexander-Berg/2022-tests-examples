jest.mock('auto-core/lib/core/isMobileApp', () => jest.fn(() => false));

import type { StatePublicUserInfoData } from 'auto-core/react/dataDomain/publicUserInfo/types';

import resellerPublicPage from './resellerPublicPage';

const pageParams = { encrypted_user_id: '123-321', category: 'all' };

describe('тексты', () => {
    it('у перекупа нормальное имя', () => {
        const state = {
            publicUserInfo: {
                data: {
                    alias: 'Normie',
                    registration_date: '2021-01-20',
                    offers_stats_by_category: {
                        ALL: {
                            active_offers_count: 13,
                            inactive_offers_count: 12,
                        },
                    } as StatePublicUserInfoData['offers_stats_by_category'],
                },
            },
        };

        const seoInfo = resellerPublicPage(state, pageParams);

        expect(seoInfo.h1).toEqual('Профессиональный продавец Normie');
        expect(seoInfo.title).toEqual('Профессиональный продавец Normie — автомобили с пробегом — 13 б/у автомобилей в наличии на Авто.ру');
        expect(seoInfo.description).toEqual('Автомобили с пробегом от профессионального продавца Normie.');
    });

    it('у перекупа айдишник вместо имени', () => {
        const state = {
            publicUserInfo: {
                data: {
                    alias: 'id132',
                    registration_date: '2021-01-20',
                    offers_stats_by_category: {
                        ALL: {
                            active_offers_count: 13,
                            inactive_offers_count: 12,
                        },
                    } as StatePublicUserInfoData['offers_stats_by_category'],
                },
            },
        };

        const seoInfo = resellerPublicPage(state, pageParams);

        expect(seoInfo.h1).toEqual('Профессиональный продавец');
        expect(seoInfo.title).toEqual('Профессиональный продавец — автомобили с пробегом — 13 б/у автомобилей в наличии на Авто.ру');
        expect(seoInfo.description).toEqual('Автомобили с пробегом от профессионального продавца.');
    });
});

describe('robots', () => {
    it('вернет noindex, если офферов меньше 30', () => {
        const state = {
            publicUserInfo: {
                data: {
                    alias: 'Normie',
                    registration_date: '2021-01-20',
                    offers_stats_by_category: {
                        ALL: {
                            active_offers_count: 13,
                            inactive_offers_count: 12,
                        },
                    } as StatePublicUserInfoData['offers_stats_by_category'],
                },
            },
        };

        const seoInfo = resellerPublicPage(state, pageParams);

        expect(seoInfo.robots).toEqual('noindex');
    });

    it('вернет noindex, если вместо имени айдишник', () => {
        const state = {
            publicUserInfo: {
                data: {
                    alias: 'id321',
                    registration_date: '2021-01-20',
                    offers_stats_by_category: {
                        ALL: {
                            active_offers_count: 34,
                            inactive_offers_count: 12,
                        },
                    } as StatePublicUserInfoData['offers_stats_by_category'],
                },
            },
        };

        const seoInfo = resellerPublicPage(state, pageParams);

        expect(seoInfo.robots).toEqual('noindex');
    });

    it('ничего не вернет, если нормальное имя и офферов больше 30', () => {
        const state = {
            publicUserInfo: {
                data: {
                    alias: 'Normie',
                    registration_date: '2021-01-20',
                    offers_stats_by_category: {
                        ALL: {
                            active_offers_count: 34,
                            inactive_offers_count: 12,
                        },
                    } as StatePublicUserInfoData['offers_stats_by_category'],
                },
            },
        };

        const seoInfo = resellerPublicPage(state, pageParams);

        expect(seoInfo.robots).toEqual('');
    });
});
