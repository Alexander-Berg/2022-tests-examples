/* eslint-disable max-len */
import { title, h1, description, footer, extraText } from '../../sites-search';

import { mockState, mockGeo } from './mock';

test('сео тексты для листинга жк', () => {
    const { searchParams, refinements, geo, user, totalOffers } = mockState;

    const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
    const testH1 = h1({ searchParams, refinements, geo, user });
    const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
    const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });
    const testExtraText = extraText({ searchParams, refinements, geo, user, totalOffers });

    expect(testTitle).toBe(
        'Купить квартиру в новостройке от застройщика, цены и планировки, объявления по продаже новых квартир в 1245 ЖК на Яндекс.Недвижимости'
    );
    expect(testH1).toBe('Купить квартиру в новостройке');
    expect(testDescription).toBe(
        'Квартиры в 1245 новых ЖК по ценам от застройщиков. Планировки, фото, описание, расположение на карте. Скидки и акции.'
    );
    expect(testFooter).toBe(
        'Все новостройки на Яндекс.Недвижимости: более 1245 ЖК . Цены на квартиры от застройщика в новостройках . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
    );
    expect(testExtraText).toBe(
        '1245 новостроек от застройщика - купить квартиру в строящемся или сданном доме с удобной планировкой.'
    );
});

test('сео тексты для листинга жк для 2х комнатных квартир', () => {
    const { searchParams, refinements, geo, user, totalOffers } = mockState;
    searchParams.roomsTotal = ['2'];

    const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
    const testH1 = h1({ searchParams, refinements, geo, user });
    const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
    const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });
    const testExtraText = extraText({ searchParams, refinements, geo, user, totalOffers });

    expect(testTitle).toBe(
        'Купить 2-комнатную квартиру в новостройке от застройщика, цены и планировки, объявления по продаже новых квартир в 1245 ЖК на Яндекс.Недвижимости'
    );
    expect(testH1).toBe('Купить 2-комнатную квартиру в новостройке');
    expect(testDescription).toBe(
        '2-комнатные квартиры в 1245 новых ЖК по ценам от застройщиков. Планировки, фото, описание, расположение на карте. Скидки и акции.'
    );
    expect(testFooter).toBe(
        'Все новостройки на Яндекс.Недвижимости: более 1245 ЖК . Цены на квартиры от застройщика в новостройках . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
    );
    expect(testExtraText).toBe(
        '1245 новостроек от застройщика - купить 2-комнатную квартиру в строящемся или сданном доме с удобной планировкой.'
    );
});

test('сео тексты для листинга жк для 4х комнатных квартир', () => {
    const { searchParams, refinements, geo, user, totalOffers } = mockState;
    searchParams.roomsTotal = ['PLUS_4'];

    const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
    const testH1 = h1({ searchParams, refinements, geo, user });
    const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
    const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });
    const testExtraText = extraText({ searchParams, refinements, geo, user, totalOffers });

    expect(testTitle).toBe(
        'Купить 4-комнатную квартиру в новостройке от застройщика, цены и планировки, объявления по продаже новых квартир в 1245 ЖК на Яндекс.Недвижимости'
    );
    expect(testH1).toBe('Купить 4-комнатную квартиру в новостройке');
    expect(testDescription).toBe(
        '4-комнатные квартиры в 1245 новых ЖК по ценам от застройщиков. Планировки, фото, описание, расположение на карте. Скидки и акции.'
    );
    expect(testFooter).toBe(
        'Все новостройки на Яндекс.Недвижимости: более 1245 ЖК . Цены на квартиры от застройщика в новостройках . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
    );
    expect(testExtraText).toBe(
        '1245 новостроек от застройщика - купить 4-комнатную квартиру в строящемся или сданном доме с удобной планировкой.'
    );
});

test('сео тексты для листинга жк, комнатности 3,4+', () => {
    const { searchParams, refinements, geo, user, totalOffers } = mockState;
    searchParams.roomsTotal = ['3', 'PLUS_4'];

    const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
    const testH1 = h1({ searchParams, refinements, geo, user });
    const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
    const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });
    const testExtraText = extraText({ searchParams, refinements, geo, user, totalOffers });

    expect(testTitle).toBe(
        'Купить многокомнатную квартиру в новостройке от застройщика, цены и планировки, объявления по продаже новых квартир в 1245 ЖК на Яндекс.Недвижимости'
    );
    expect(testH1).toBe('Купить многокомнатную квартиру в новостройке');
    expect(testDescription).toBe(
        'Многокомнатные квартиры в 1245 новых ЖК по ценам от застройщиков. Планировки, фото, описание, расположение на карте. Скидки и акции.'
    );
    expect(testFooter).toBe(
        'Все новостройки на Яндекс.Недвижимости: более 1245 ЖК . Цены на квартиры от застройщика в новостройках . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
    );
    expect(testExtraText).toBe(
        '1245 новостроек от застройщика - купить многокомнатную квартиру в строящемся или сданном доме с удобной планировкой.'
    );
});

test('сео тексты для листинга жк, комнатности 1,2,3', () => {
    const { searchParams, refinements, geo, user, totalOffers } = mockState;
    searchParams.roomsTotal = ['1', '2', '3'];

    const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
    const testH1 = h1({ searchParams, refinements, geo, user });
    const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
    const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });
    const testExtraText = extraText({ searchParams, refinements, geo, user, totalOffers });

    expect(testTitle).toBe(
        'Купить квартиру в новостройке от застройщика, цены и планировки, объявления по продаже новых квартир в 1245 ЖК на Яндекс.Недвижимости'
    );
    expect(testH1).toBe('Купить квартиру в новостройке');
    expect(testDescription).toBe(
        'Квартиры в 1245 новых ЖК по ценам от застройщиков. Планировки, фото, описание, расположение на карте. Скидки и акции.'
    );
    expect(testFooter).toBe(
        'Все новостройки на Яндекс.Недвижимости: более 1245 ЖК . Цены на квартиры от застройщика в новостройках . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
    );
    expect(testExtraText).toBe(
        '1245 новостроек от застройщика - купить квартиру в строящемся или сданном доме с удобной планировкой.'
    );
});

test('сео тексты для листинга жк, комнатности 1,2,4+', () => {
    const { searchParams, refinements, geo, user, totalOffers } = mockState;
    searchParams.roomsTotal = ['1', '2', 'PLUS_4'];

    const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
    const testH1 = h1({ searchParams, refinements, geo, user });
    const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
    const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });
    const testExtraText = extraText({ searchParams, refinements, geo, user, totalOffers });

    expect(testTitle).toBe(
        'Купить многокомнатную квартиру в новостройке от застройщика, цены и планировки, объявления по продаже новых квартир в 1245 ЖК на Яндекс.Недвижимости'
    );
    expect(testH1).toBe('Купить многокомнатную квартиру в новостройке');
    expect(testDescription).toBe(
        'Многокомнатные квартиры в 1245 новых ЖК по ценам от застройщиков. Планировки, фото, описание, расположение на карте. Скидки и акции.'
    );
    expect(testFooter).toBe(
        'Все новостройки на Яндекс.Недвижимости: более 1245 ЖК . Цены на квартиры от застройщика в новостройках . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
    );
    expect(testExtraText).toBe(
        '1245 новостроек от застройщика - купить многокомнатную квартиру в строящемся или сданном доме с удобной планировкой.'
    );
});

test('сео тексты для листинга жк, выбраны все комнатности', () => {
    const { searchParams, refinements, geo, user, totalOffers } = mockState;
    searchParams.roomsTotal = ['1', '2', '3', 'PLUS_4'];

    const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
    const testH1 = h1({ searchParams, refinements, geo, user });
    const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
    const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });
    const testExtraText = extraText({ searchParams, refinements, geo, user, totalOffers });

    expect(testTitle).toBe(
        'Купить многокомнатную квартиру в новостройке от застройщика, цены и планировки, объявления по продаже новых квартир в 1245 ЖК на Яндекс.Недвижимости'
    );
    expect(testH1).toBe('Купить многокомнатную квартиру в новостройке');
    expect(testDescription).toBe(
        'Многокомнатные квартиры в 1245 новых ЖК по ценам от застройщиков. Планировки, фото, описание, расположение на карте. Скидки и акции.'
    );
    expect(testFooter).toBe(
        'Все новостройки на Яндекс.Недвижимости: более 1245 ЖК . Цены на квартиры от застройщика в новостройках . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
    );
    expect(testExtraText).toBe(
        '1245 новостроек от застройщика - купить многокомнатную квартиру в строящемся или сданном доме с удобной планировкой.'
    );
});

test('сео тексты для листинга жк со сроком сдачи', () => {
    const { refinements, geo, user, totalOffers } = mockState;
    const searchParams = {
        rgid: 587795,
        type: 'SELL',
        deliveryDate: '4_2022',
    };

    const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
    const testH1 = h1({ searchParams, refinements, geo, user });
    const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
    const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });
    const testExtraText = extraText({ searchParams, refinements, geo, user, totalOffers });

    expect(testTitle).toBe(
        'Купить квартиру в новостройке со сроком сдачи в 2022 году — Свежие объявления по продаже новых квартир в 1245 ЖК на Яндекс.Недвижимости'
    );
    expect(testH1).toBe('Новостройки  со сроком сдачи 2022 год');
    expect(testDescription).toBe(
        'Свежие объявления по продаже квартир в 1245 ЖК со сроком сдачи в 2022 году. Карты цен, инфраструктуры, расчет времени на дорогу на Яндекс.Недвижимости.'
    );
    expect(testFooter).toBe(
        'Все новостройки на Яндекс.Недвижимости: более 1245 ЖК . Цены на квартиры от застройщика в новостройках со сроком сдачи до 4 квартала 2022 . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
    );

    expect(testExtraText).toBe(
        '1245 новостроек от застройщика - купить квартиру со сроком сдачи 2022 год с удобной планировкой.'
    );
});

test('сео тексты для листинга жк со сроком сдачи NOT_FINISHED', () => {
    const { refinements, geo, user, totalOffers } = mockState;
    const searchParams = {
        rgid: 587795,
        type: 'SELL',
        deliveryDate: 'NOT_FINISHED',
    };

    const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
    const testH1 = h1({ searchParams, refinements, geo, user });
    const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
    const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });
    const testExtraText = extraText({ searchParams, refinements, geo, user, totalOffers });

    expect(testTitle).toBe(
        'Купить квартиру в строящейся новостройке от застройщика, цены и планировки, объявления по продаже новых квартир в 1245 строящихся ЖК на Яндекс.Недвижимости'
    );
    expect(testH1).toBe('Купить квартиру в строящемся доме');
    expect(testDescription).toBe(
        'Квартиры в 1245 новых строящихся ЖК по ценам от застройщиков. Планировки, фото, описание, расположение на карте. Скидки и акции.'
    );
    expect(testFooter).toBe(
        'Все новостройки на Яндекс.Недвижимости: более 1245 строящихся ЖК . Цены на квартиры от застройщика в строящемся доме . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
    );
    expect(testExtraText).toBe(
        '1245 новостроек от застройщика - купить квартиру в строящемся доме с удобной планировкой.'
    );
});

test('сео тексты для листинга жк со сроком сдачи FINISHED', () => {
    const { refinements, geo, user, totalOffers } = mockState;
    const searchParams = {
        rgid: 587795,
        type: 'SELL',
        deliveryDate: 'FINISHED',
    };

    const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
    const testH1 = h1({ searchParams, refinements, geo, user });
    const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
    const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });
    const testExtraText = extraText({ searchParams, refinements, geo, user, totalOffers });

    expect(testTitle).toBe(
        'Купить квартиру в новостройке от застройщика с ключами, цены и планировки, объявления по продаже новых квартир в 1245 ЖК на Яндекс.Недвижимости'
    );
    expect(testH1).toBe('Купить квартиру с ключами в сданном доме');
    expect(testDescription).toBe(
        'Квартиры с ключами в 1245 новых ЖК по ценам от застройщиков. Планировки, фото, описание, расположение на карте. Скидки и акции.'
    );
    expect(testFooter).toBe(
        'Все новостройки на Яндекс.Недвижимости: более 1245 ЖК . Цены на квартиры от застройщика в сданном доме . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
    );
    expect(testExtraText).toBe(
        '1245 новостроек от застройщика - купить квартиру с ключами в сданном доме с удобной планировкой.'
    );
});

test('extraText дополнительные кейсы', () => {
    const { refinements, user } = mockState;
    const commonParams = { geo: mockGeo, user, refinements };

    expect(
        extraText({
            ...commonParams,
            searchParams: {
                priceMin: 10000000,
                roomsTotal: 'STUDIO',
            },
            totalOffers: 23,
        })
    ).toBe(
        '23 новостройки от застройщика в Москве - купить квартиру-студию по цене от 10 млн ₽ в строящемся или сданном доме с удобной планировкой.'
    );

    expect(
        extraText({
            ...commonParams,
            searchParams: {
                roomsTotal: ['PLUS_4', 'STUDIO'],
            },
            totalOffers: 25,
        })
    ).toBe(
        '25 новостроек от застройщика в Москве - купить  квартиру в строящемся или сданном доме с удобной планировкой.'
    );

    expect(
        extraText({
            ...commonParams,
            searchParams: {
                deliveryDate: '2_2022',
                developerId: 52308,
            },
            totalOffers: 29,
            searchQuery: { developerName: 'ПИК' },
        })
    ).toBe(
        '29 новостроек в Москве - купить квартиру со сроком сдачи до 2 квартала 2022 от застройщика ПИК с удобной планировкой.'
    );

    expect(
        extraText({
            ...commonParams,
            searchParams: {
                deliveryDate: '4_2022',
                developerId: 52308,
            },
        })
    ).toBe('Новостройки в Москве - купить квартиру со сроком сдачи 2022 год с удобной планировкой.');
});
