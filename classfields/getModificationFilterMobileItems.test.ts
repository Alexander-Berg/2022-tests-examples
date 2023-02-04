import cardGroupComplectationsMock from 'autoru-frontend/mockData/state/cardGroupComplectations.mock';

import getModificationFilterMobileItems from './getModificationFilterMobileItems';

const state = {
    ...cardGroupComplectationsMock,
};

it('должен вернуть правильный список значений фильтра модификации для мобилки', () => {
    expect(getModificationFilterMobileItems(state)).toStrictEqual([
        {
            id: '21221591',
            title: '2.5 AT (181 л.с.)',
        },
        {
            id: '21221553',
            title: '2.0 AT (150 л.с.)',
        },
    ]);
});
