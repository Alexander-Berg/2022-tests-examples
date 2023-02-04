import getListingTitleTagsText from './getListingTitleTagsText';

it('возвращает заголовки тегов в виде строки', () => {
    const tags = [
        {
            isHot: false,
            title: 'Новости',
            urlPart: 'news',
        },
        {
            isHot: false,
            title: 'BMW',
            urlPart: 'bmw',
        },
    ];

    const result = getListingTitleTagsText(tags);

    expect(result).toBe('Новости BMW');
});
