import tagsMock from 'auto-core/react/dataDomain/mag/mocks/tags';

import prepareNavigationTags from './prepareNavigationTags';

it('ничего не подготавливает, если передать пустой массив тегов', () => {
    expect(prepareNavigationTags([])).toHaveLength(0);
});

it('подготавливает переданные теги', () => {
    expect(prepareNavigationTags(tagsMock)).toMatchSnapshot();
});
