import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import shouldShowBeatenOrCustomsNotClearedRemoveActions from './shouldShowBeatenOrCustomsNotClearedRemoveActions';

const defaultHasExperiment = (exp: string) => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN';

it('вернет true если эксп есть, секция не новые, категория легковые и damage_group=ANY', () => {
    const searchParams: TSearchParameters = {
        section: 'used',
        category: 'cars',
        damage_group: 'ANY',
    };

    const results = shouldShowBeatenOrCustomsNotClearedRemoveActions(searchParams, defaultHasExperiment);
    expect(results).toBe(true);
});

it('вернет true если эксп есть, секция не новые, категория легковые и customs_state_group=DOESNT_MATTER', () => {
    const searchParams: TSearchParameters = {
        section: 'used',
        category: 'cars',
        customs_state_group: 'DOESNT_MATTER',
    };

    const results = shouldShowBeatenOrCustomsNotClearedRemoveActions(searchParams, defaultHasExperiment);
    expect(results).toBe(true);
});

it('вернет true если эксп есть, секция не новые, категория не легковые и damage_group=ANY', () => {
    const searchParams: TSearchParameters = {
        section: 'used',
        category: 'moto',
        damage_group: 'ANY',
    };

    const results = shouldShowBeatenOrCustomsNotClearedRemoveActions(searchParams, defaultHasExperiment);
    expect(results).toBe(true);
});

it('вернет false если нет экспа', () => {
    const searchParams: TSearchParameters = {
        section: 'used',
        category: 'cars',
        damage_group: 'ANY',
    };

    const results = shouldShowBeatenOrCustomsNotClearedRemoveActions(searchParams, () => false);
    expect(results).toBe(false);
});

it('вернет false если секция новые', () => {
    const searchParams: TSearchParameters = {
        section: 'new',
        category: 'cars',
        damage_group: 'ANY',
    };

    const results = shouldShowBeatenOrCustomsNotClearedRemoveActions(searchParams, defaultHasExperiment);
    expect(results).toBe(false);
});

it('вернет false категория не легковые и customs_state_group=DOESNT_MATTER', () => {
    const searchParams: TSearchParameters = {
        section: 'used',
        category: 'moto',
        customs_state_group: 'DOESNT_MATTER',
    };

    const results = shouldShowBeatenOrCustomsNotClearedRemoveActions(searchParams, defaultHasExperiment);
    expect(results).toBe(false);
});

it('вернет false нет ни damage_group=ANY, ни customs_state_group=DOESNT_MATTER', () => {
    const searchParams: TSearchParameters = {
        section: 'used',
        category: 'cars',
        damage_group: 'BEATEN',
        customs_state_group: 'CLEARED',
    };

    const results = shouldShowBeatenOrCustomsNotClearedRemoveActions(searchParams, defaultHasExperiment);
    expect(results).toBe(false);
});
