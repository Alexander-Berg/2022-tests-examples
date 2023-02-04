import _ from 'lodash';

import type { PaidService } from 'auto-core/types/proto/auto/api/api_offer_model';
import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import { offerStatsDayMock } from 'auto-core/models/offerStats/mocks';
import type { OfferStatsDay } from 'auto-core/models/offerStats/types';

import hasVasPromoOnLastDay from './hasVasPromoOnLastDay';

const baseCounters = [
    offerStatsDayMock.withViews(100).withDateFromToday(-6).value(),
    offerStatsDayMock.withViews(65).withDateFromToday(-5).withServices([ { service: TOfferVas.FRESH, is_active: true } as PaidService ]).value(),
    offerStatsDayMock.withViews(57).withDateFromToday(-4).value(),
    offerStatsDayMock.withViews(5).withDateFromToday(-3).value(),
    offerStatsDayMock.withViews(11).withDateFromToday(-2).withServices([ { service: TOfferVas.BADGE, is_active: true } as PaidService ]).value(),
    offerStatsDayMock.withViews(42).withDateFromToday(-1).withServices([ { service: TOfferVas.PLACEMENT, is_active: true } as PaidService ]).value(),
    offerStatsDayMock.withViews(13).withDateFromToday(0).value(),
];

it('вернет true при выполнении всех условий', () => {
    expect(hasVasPromoOnLastDay(baseCounters)).toBe(true);
});

describe('вернет false', () => {
    let counters: Array<OfferStatsDay>;

    beforeEach(() => {
        counters = _.cloneDeep(baseCounters);
    });

    it('если число дней меньше 4', () => {
        counters = _.take(counters, 3);

        expect(hasVasPromoOnLastDay(counters)).toBe(false);
    });

    it('в один из последних трех дней действовал вас', () => {
        counters[counters.length - 2] = offerStatsDayMock
            .withViews(42)
            .withDateFromToday(-1)
            .withServices([ { service: TOfferVas.FRESH, is_active: true } as PaidService ])
            .value();

        expect(hasVasPromoOnLastDay(counters)).toBe(false);
    });

    it('кол-во просмотров сегодня входит в ТОП-3', () => {
        counters[counters.length - 1].views = 60;

        expect(hasVasPromoOnLastDay(counters)).toBe(false);
    });
});
