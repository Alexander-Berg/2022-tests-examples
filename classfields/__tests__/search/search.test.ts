/* eslint-disable max-len */
import { descriptionBuild, extraTextBuild, footerTextBuild, titleBuild } from '../../search';

import { mockState } from './mock';

test('Новый текст для дескрипшн с эмодзи независимо от гео', () => {
    const {
        searchParams,
        pageType,
        refinements,
        user,
        totalOffers,
        infoAboutTotalItems
    } = mockState;
    const description = descriptionBuild(
        searchParams,
        pageType,
        refinements,
        {},
        user,
        totalOffers,

        infoAboutTotalItems
    );

    expect(description).toBe(
        '🏠 1 245 свежих объявлений по продаже квартир. ➜ Купите свою лучшую квартиру по цене от 55 555 ₽ из 1 245 объявлений по продаже недвижимости '
    );
});

test('Новый текст для дескрипшн с эмодзи независимо от гео без минимальной цены', () => {
    const { searchParams, pageType, refinements, user, totalOffers } = mockState;

    const description = descriptionBuild(
        searchParams,
        pageType,
        refinements,
        {},
        user,
        totalOffers,
        null
    );

    expect(description).toBe(
        '🏠 1 245 свежих объявлений по продаже квартир. ➜ Купите свою лучшую квартиру. Объявления по продаже недвижимости '
    );
});

test('Новый текст для дескрипшн с эмодзи независимо от гео на пустом листинге', () => {
    const { searchParams, pageType, refinements, user, infoAboutTotalItems } = mockState;
    const description = descriptionBuild(
        searchParams,
        pageType,
        refinements,
        {},
        user,
        0,
        infoAboutTotalItems
    );

    expect(description).toBe(
        '🏠 Объявления по продаже квартир. ➜ Купите свою лучшую квартиру. Объявления по продаже недвижимости '
    );
});

describe('Сео тексты на листинг с параметрами улица + дом', () => {
    test('для покупки недвижмости', () => {
        const { searchParams, pageType, geo, user, infoAboutTotalItems } = mockState;

        const modifiedSearchParams = {
            ...searchParams,
            streetId: 62613,
            streetName: 'ulica-arbat',
            buildingIds: '8017417998175267261',
            houseNumber: '24',
        };

        const refinements = {
            street: {
                _name: 'street',
                shortName: '1 улица',
                name: 'адрес',
                list: [
                    {
                        name: 'улица Арбат',
                        id: 62613,
                        buildingIds: '8017417998175267261',
                        houseNumber: '24',
                    },
                ],
            },
        };

        const description = descriptionBuild(
            modifiedSearchParams,
            pageType,
            refinements,
            geo,
            user,
            12,
            infoAboutTotalItems
        );

        const title = titleBuild(modifiedSearchParams, refinements, geo, user, 12);
        const footerText = footerTextBuild(modifiedSearchParams, refinements, geo, user, 12, infoAboutTotalItems);
        const extraText = extraTextBuild(modifiedSearchParams, refinements, geo, user, 12, infoAboutTotalItems);

        expect(title).toBe(
            'Купить квартиру - улица Арбат, дом 24, Москва - объявления по продаже квартир на сайте Яндекс.Недвижимость'
        );
        expect(description).toBe(
            '🏠 Объявления по продаже квартир - улица Арбат, дом 24, Москва. ➜ Купите свою лучшую квартиру по цене от 55 555 ₽ из 12 объявлений по продаже недвижимости в Москве'
        );
        expect(footerText).toBe(
            'Купить квартиру - 12 объявлений от агентств и собственников по продаже квартир - улица Арбат, дом 24, Москва на Яндекс.Недвижимости. Выберите лучшую квартиру в Москве по цене от 55 555 ₽.'
        );
        expect(extraText).toBe(
            'Купить квартиру - 12 объявлений от агентств и собственников по продаже квартир - улица Арбат, дом 24, Москва по цене от 55 555 ₽.'
        );
    });

    test('для аренды коммерческого помещения', () => {
        const { searchParams, pageType, geo, user, infoAboutTotalItems } = mockState;

        const modifiedSearchParams = {
            ...searchParams,
            category: 'COMMERCIAL',
            type: 'RENT',
            streetId: 164249,
            streetName: 'ulica-chayanova',
            buildingIds: '8732880288045867978',
            houseNumber: '7',
        };

        const refinements = {
            street: {
                _name: 'street',
                shortName: '1 улица',
                name: 'адрес',
                list: [
                    {
                        name: 'улица Чаянова',
                        id: 164249,
                        buildingIds: '8732880288045867978',
                        houseNumber: '7',
                    },
                ],
            },
        };

        const description = descriptionBuild(
            modifiedSearchParams,
            pageType,
            refinements,
            geo,
            user,
            24,
            infoAboutTotalItems
        );

        const title = titleBuild(modifiedSearchParams, refinements, geo, user, 24);
        const footerText = footerTextBuild(modifiedSearchParams, refinements, geo, user, 24, infoAboutTotalItems);
        const extraText = extraTextBuild(modifiedSearchParams, refinements, geo, user, 24, infoAboutTotalItems);

        expect(title).toBe(
            'Снять коммерческую недвижимость - улица Чаянова, дом 7, Москва - объявления по аренде коммерческой недвижимости на сайте Яндекс.Недвижимость'
        );
        expect(description).toBe(
            '🏢 Объявления по аренде коммерческой недвижимости - улица Чаянова, дом 7, Москва. ➜ Снимите свою лучшую коммерческую недвижимость по цене от 55 555 ₽ из 24 объявлений по аренде недвижимости в Москве'
        );
        expect(footerText).toBe(
            'Снять коммерческую недвижимость - 24 объявления от агентств и собственников по аренде коммерческой недвижимости - улица Чаянова, дом 7, Москва на Яндекс.Недвижимости. Выберите лучшую коммерческую недвижимость в Москве по цене от 55 555 ₽ в месяц.'
        );
        expect(extraText).toBe(
            'Снять коммерческую недвижимость - 24 объявления от агентств и собственников по аренде коммерческой недвижимости - улица Чаянова, дом 7, Москва по цене от 55 555 ₽ в месяц.'
        );
    });
});
