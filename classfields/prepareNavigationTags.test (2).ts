import tagsMock from 'auto-core/react/dataDomain/mag/mocks/tags';

import prepareNavigationTags from './prepareNavigationTags';

it('ничего не подготавливает, если передать пустой массив тегов', () => {
    expect(prepareNavigationTags([])).toHaveLength(0);
});

it('подготавливает только зафиксированные теги', () => {
    expect(prepareNavigationTags([], true)).toHaveLength(1);
});

it('подготавливает переданные теги', () => {
    expect(prepareNavigationTags(tagsMock)).toMatchSnapshot();
});

it('подготавливает переданные и зафиксированные теги', () => {
    expect(prepareNavigationTags(tagsMock, true)).toMatchSnapshot();
});
