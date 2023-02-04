import cardGroupComplectationsMock from 'autoru-frontend/mockData/state/cardGroupComplectations.mock';

import getModificationFilterItems from './getModificationFilterItems';

const state = {
    ...cardGroupComplectationsMock,
};

it('должен вернуть правильный список значений фильтра модификации', () => {
    expect(getModificationFilterItems(state)).toStrictEqual([
        {
            id: '21221591',
            title: '2.5 л (181 л.с.) Бензин / Автоматическая / Передний',
        },
        {
            id: '21221553',
            title: '2.0 л (150 л.с.) Бензин / Автоматическая / Передний',
        },
    ]);
});
