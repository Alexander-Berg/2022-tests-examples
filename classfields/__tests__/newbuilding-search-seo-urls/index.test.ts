import { newbuildingSearchSeoUrlsProvider } from 'realty-core/app/providers/newbuilding-search-seo-urls';

import { getMockData } from './mocks';

it('Десктоп. Офферов >= 3. Не должен обрезать ampUrl и canonicalUrl.', () => {
    const data = getMockData('desktop', 100, false);
    newbuildingSearchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/kupit/novostrojka/st-ulica-cyurupy-55034/?newFlat=YES';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/novostrojka/st-ulica-cyurupy-55034/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('Десктоп. Офферов < 3. Должен обрезать ampUrl и canonicalUrl. Остается только тип сделки и тип недвиги.', () => {
    const data = getMockData('desktop', 2, false);
    newbuildingSearchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/kupit/novostrojka/';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/novostrojka/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('Тач. Офферов >= 3. Не должен обрезать ampUrl и canonicalUrl.', () => {
    const data = getMockData('touch-phone', 100, false);
    newbuildingSearchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/kupit/novostrojka/st-ulica-cyurupy-55034/?newFlat=YES';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/novostrojka/st-ulica-cyurupy-55034/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('Тач. Офферов < 3. Должен обрезать ampUrl и canonicalUrl. Остается только тип сделки и тип недвиги.', () => {
    const data = getMockData('touch-phone', 2, false);
    newbuildingSearchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/kupit/novostrojka/';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/novostrojka/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('AMP. Офферов >= 3. Не должен обрезать canonicalUrl.', () => {
    const data = getMockData('desktop', 100, true);
    newbuildingSearchSeoUrlsProvider.call(data, data);

    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/novostrojka/st-ulica-cyurupy-55034/';
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('AMP. Офферов < 3. Должен обрезать canonicalUrl. Остается только тип сделки и тип недвиги.', () => {
    const data = getMockData('desktop', 2, true);
    newbuildingSearchSeoUrlsProvider.call(data, data);

    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/novostrojka/';
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});
