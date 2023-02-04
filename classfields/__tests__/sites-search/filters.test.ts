/* eslint-disable max-len */
import { title, h1, description, footer } from '../../sites-search';

import { mockState } from './mock';
import { DECORATION_FILTER_VALUES, decorationSeoTextsByValue } from './helpers/filters';

describe('Фильтры', () => {
    describe('Отделка', () => {
        it.each(DECORATION_FILTER_VALUES)('?decoration=%s', (decoration) => {
            const { searchParams, refinements, geo, user, totalOffers } = mockState;
            searchParams.decoration = decoration;

            const testTitle = title({ searchParams, refinements, geo, user, totalOffers });
            const testH1 = h1({ searchParams, refinements, geo, user });
            const testDescription = description({ searchParams, refinements, geo, user, totalOffers });
            const testFooter = footer({ searchParams, refinements, geo, user, totalOffers });

            const seoText = decorationSeoTextsByValue[decoration];

            expect(testTitle).toBe(
                `Купить квартиру в новостройке от застройщика ${seoText}, цены и планировки, объявления по продаже новых квартир в 1245 ЖК на Яндекс.Недвижимости`
            );
            expect(testH1).toBe(`Купить квартиру ${seoText} в новостройке`);
            expect(testDescription).toBe(
                `Квартиры ${seoText} в 1245 новых ЖК по ценам от застройщиков. Планировки, фото, описание, расположение на карте. Скидки и акции.`
            );
            expect(testFooter).toBe(
                'Все новостройки на Яндекс.Недвижимости: более 1245 ЖК . Цены на квартиры с отделкой от застройщика в новостройках . Быстрый поиск квартир с удобной сортировкой и фильтрацией по вашим требованиям.'
            );
        });
    });
});
