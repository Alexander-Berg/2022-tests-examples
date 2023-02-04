import aliases from 'realty-router/lib/filters/aliases';

import { getFilterAliasesRouterByCategory } from '../getFilterAliasesRouterByCategory';

it('возвращает пустой массив, если нет категории', () => {
    const result = getFilterAliasesRouterByCategory();

    expect(result).toEqual([]);
});

it('возвращает алиасы фильтров по списку фильтров', () => {
    const result = getFilterAliasesRouterByCategory([
        {
            params: [],
            filters: [
                aliases.ROUGH_DECORATION,
                aliases.CLEAN_DECORATION,
                aliases.TURNKEY_DECORATION,
                aliases.PRE_CLEAN_DECORATION,
                aliases.WHITE_BOX_DECORATION,
                aliases.NO_DECORATION,

                aliases.BESIDE_METRO,

                aliases.APARTMENTS,
            ],
        },
    ]);

    expect(result).toEqual([
        'chernovaya-otdelka',
        'chistovaya-otdelka',
        'pod-kluch',
        'predchistovya-otdelka',
        'white-box',
        'bez-otdelky',
        'ryadom-metro',
        'apartamenty',
    ]);
});
