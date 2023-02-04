import { getLDJson } from './getLDJson';

const ITEMS_MOCK = [
    { href: 'https://realty.yandex.ru', isHostItem: true, text: 'Я.Недвижимость' },
    { href: '/journal/', text: 'Журнал' },
    { href: '/journal/category/uchebnik/', text: 'Учебник' },
    { text: 'Аренда' },
];

it('возвращает LDJson для ХК', () => {
    const wrapper = getLDJson(ITEMS_MOCK, 'https://example.com/');

    expect(wrapper).toEqual({
        '@context': 'https://schema.org',
        '@type': 'BreadcrumbList',
        itemListElement: [
            {
                '@type': 'ListItem',
                item: 'https://realty.yandex.ru',
                name: 'Я.Недвижимость',
                position: 1,
            },
            {
                '@type': 'ListItem',
                item: 'https://realty.yandex.ru/journal/',
                name: 'Журнал',
                position: 2,
            },
            {
                '@type': 'ListItem',
                item: 'https://realty.yandex.ru/journal/category/uchebnik/',
                name: 'Учебник',
                position: 3,
            },
            {
                '@type': 'ListItem',
                item: 'https://example.com/',
                name: 'Аренда',
                position: 4,
            },
        ],
    });
});
