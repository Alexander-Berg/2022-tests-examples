import { offersSearchSeoTextsSelector } from '../seoTexts';

import { mockState, mockStateNonResidential, mockStateWithFilters, mockStateWithoutMinPrice } from './mockState';

describe('Touch Phone search seo texts', function() {
    it('texts values on first mock data (just search)', function() {
        const commonSearchSelectorResult = offersSearchSeoTextsSelector(mockState);

        /* eslint-disable max-len */
        expect(commonSearchSelectorResult.title).toBe('Купить квартиру в Москве и МО - 26 267 объявлений по продаже квартир на сайте Яндекс.Недвижимость');
        expect(commonSearchSelectorResult.description).toBe('🏠 26 267 свежих объявлений по продаже квартир в Москве и МО. ➜ Купите свою лучшую квартиру по цене от 50 000 ₽ из 26 267 объявлений по продаже недвижимости в Москве и МО');
        expect(commonSearchSelectorResult.h1).toBe('Купить квартиру в Москве и МО');
        expect(commonSearchSelectorResult.footerText).toBe('Купить квартиру - 26 267 объявлений от агентств и собственников по продаже квартир в Москве и МО на Яндекс.Недвижимости. Выберите лучшую квартиру в Москве и МО по цене от 50 000 ₽.');
        /* eslint-enable max-len */
    });

    it('texts values on second mock data (search with filters)', function() {
        const commonSearchSelectorResult = offersSearchSeoTextsSelector(mockStateWithFilters);

        /* eslint-disable max-len */
        expect(commonSearchSelectorResult.title).toBe('Купить 2-комнатную квартиру от 1,23 млн рублей в Москве и МО - 5 632 объявления по продаже двухкомнатных квартир от 1,23 млн рублей на сайте Яндекс.Недвижимость');
        expect(commonSearchSelectorResult.description).toBe('🏠 5 632 свежих объявления по продаже 2-комнатных квартир от 1,23 млн рублей в Москве и МО. ➜ Купите свою лучшую квартиру по цене от 3 802 277 ₽ из 5 632 объявлений по продаже недвижимости в Москве и МО');
        expect(commonSearchSelectorResult.h1).toBe('Купить 2-комнатную квартиру от 1,23 млн рублей в Москве и МО');
        expect(commonSearchSelectorResult.footerText).toBe('Купить 2-комнатную квартиру - 5 632 объявления от агентств и собственников по продаже квартир в Москве и МО от 1,23 млн рублей на Яндекс.Недвижимости. Выберите лучшую 2-комнатную квартиру в Москве и МО.');
        /* eslint-enable max-len */
    });

    it('texts values on third mock data (non-residential search)', function() {
        const commonSearchSelectorResult = offersSearchSeoTextsSelector(mockStateNonResidential);

        /* eslint-disable max-len */
        expect(commonSearchSelectorResult.title).toBe('Купить коммерческую недвижимость от 4 рублей в Москве и МО - объявления по продаже коммерческой недвижимости от 4 рублей на сайте Яндекс.Недвижимость');
        expect(commonSearchSelectorResult.description).toBe('🏢 Объявления по продаже коммерческой недвижимости от 4 рублей в Москве и МО. ➜ Купите свою лучшую коммерческую недвижимость по цене от 3 400 000 ₽ из 4 объявлений по продаже недвижимости в Москве и МО');
        expect(commonSearchSelectorResult.h1).toBe('Купить коммерческую недвижимость от 4 рублей в Москве и МО');
        expect(commonSearchSelectorResult.footerText).toBe('Купить коммерческую недвижимость - 4 объявления от агентств и собственников по продаже коммерческой недвижимости в Москве и МО от 4 рублей на Яндекс.Недвижимости. Выберите лучшую коммерческую недвижимость в Москве и МО.');
        /* eslint-enable max-len */
    });

    it('texts values on fourth mock data (residential search without min price)', function() {
        const commonSearchSelectorResult = offersSearchSeoTextsSelector(mockStateWithoutMinPrice);

        /* eslint-disable max-len */
        expect(commonSearchSelectorResult.title).toBe('Купить квартиру в Москве и МО - 26 267 объявлений по продаже квартир на сайте Яндекс.Недвижимость');
        expect(commonSearchSelectorResult.description).toBe('🏠 26 267 свежих объявлений по продаже квартир в Москве и МО. ➜ Купите свою лучшую квартиру. Объявления по продаже недвижимости в Москве и МО');
        expect(commonSearchSelectorResult.h1).toBe('Купить квартиру в Москве и МО');
        expect(commonSearchSelectorResult.footerText).toBe('Купить квартиру - 26 267 объявлений от агентств и собственников по продаже квартир в Москве и МО на Яндекс.Недвижимости. Выберите лучшую квартиру в Москве и МО.');
        /* eslint-enable max-len */
    });
});
