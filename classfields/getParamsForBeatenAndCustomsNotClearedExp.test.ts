import type { TSearchParameters, Section } from 'auto-core/types/TSearchParameters';
import type { TOfferCategory } from 'auto-core/types/proto/auto/api/api_offer_model';

import getParamsForBeatenAndCustomsNotClearedExp from './getParamsForBeatenAndCustomsNotClearedExp';

const defaultSearchParams: TSearchParameters = {
    damage_group: 'ANY',
    customs_state_group: 'DOESNT_MATTER',
    category: 'cars' as TOfferCategory,
    section: 'used' as Section,
    price_to: 999999,
};

it('должен убрать только damage_group=ANY и customs_state_group=DOESNT_MATTER', () => {
    expect(getParamsForBeatenAndCustomsNotClearedExp(defaultSearchParams, true)).toEqual({
        category: 'cars',
        section: 'used',
        price_to: 999999,
    });
});

it('должен вернуть изначальные параметры, если не соответствует секция', () => {
    const searchParams = {
        ...defaultSearchParams,
        section: 'new' as Section,
    };

    expect(getParamsForBeatenAndCustomsNotClearedExp(searchParams, true)).toEqual(searchParams);
});

it('должен вернуть изначальные параметры, если не в экспе', () => {
    expect(getParamsForBeatenAndCustomsNotClearedExp(defaultSearchParams, false)).toEqual(defaultSearchParams);
});

it('должен добавить damage_group=NOT_BEATEN и customs_state_group=CLEARED, если какого-то из них нет', () => {
    const params = {
        category: 'cars' as TOfferCategory,
        section: 'used' as Section,
        price_to: 999999,
    };
    expect(getParamsForBeatenAndCustomsNotClearedExp(params, true)).toEqual({
        category: 'cars',
        section: 'used',
        price_to: 999999,
        customs_state_group: 'CLEARED',
        damage_group: 'NOT_BEATEN',
    });
});
