import type { UserInfoResponse } from '@vertis/schema-registry/ts-types-snake/auto/api/response_model';
import { ResponseStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/response_model';

import preparePublicUserInfo from './preparePublicUserInfo';

const responseValue: UserInfoResponse = {
    alias: 'Charlie Sheen',
    registration_date: '01-13-2021',
    share_url: 'share_url',
    offers_stats_by_category: {
        MOTO: {
            active_offers_count: 7,
            inactive_offers_count: 6,
        },
        CARS: {
            active_offers_count: 3,
            inactive_offers_count: 0,
        },
        TRUCKS: {
            active_offers_count: 0,
            inactive_offers_count: 8,
        },
    },
    status: ResponseStatus.SUCCESS,
};

it('должен добавить категорию ALL, в которой просуммированы все остальные каунтеры', () => {
    const preparedData = preparePublicUserInfo({ result: responseValue });

    expect(preparedData).toEqual({
        alias: 'Charlie Sheen',
        registration_date: '01-13-2021',
        share_url: 'share_url',
        offers_stats_by_category: {
            MOTO: {
                active_offers_count: 7,
                inactive_offers_count: 6,
            },
            CARS: {
                active_offers_count: 3,
                inactive_offers_count: 0,
            },
            TRUCKS: {
                active_offers_count: 0,
                inactive_offers_count: 8,
            },
            ALL: {
                active_offers_count: 10,
                inactive_offers_count: 14,
            },
        },
        status: 'SUCCESS',
    });
});
