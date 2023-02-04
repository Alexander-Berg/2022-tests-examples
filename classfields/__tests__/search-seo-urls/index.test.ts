import { searchSeoUrlsProvider } from 'realty-core/app/providers/search-seo-urls';

import { getMockData } from './mocks';

it('Десктоп. Офферов >= 3. Продажа. Не должен обрезать ampUrl и canonicalUrl.', () => {
    const data = getMockData('SELL', 'desktop', 100, false);
    searchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/kupit/kvartira/st-ulica-cyurupy-55034/';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/kvartira/st-ulica-cyurupy-55034/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

// eslint-disable-next-line max-len
it('Десктоп. Офферов < 3. Продажа. Должен обрезать ampUrl и canonicalUrl. Остается только тип сделки и тип недвиги.', () => {
    const data = getMockData('SELL', 'desktop', 2, false);
    searchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/kupit/kvartira/';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/kvartira/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('Тач. Офферов >= 3. Продажа. Не должен обрезать ampUrl и canonicalUrl.', () => {
    const data = getMockData('SELL', 'touch-phone', 100, false);
    searchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/kupit/kvartira/st-ulica-cyurupy-55034/';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/kvartira/st-ulica-cyurupy-55034/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

// eslint-disable-next-line max-len
it('Тач. Офферов < 3. Продажа. Должен обрезать ampUrl и canonicalUrl. Остается только тип сделки и тип недвиги.', () => {
    const data = getMockData('SELL', 'touch-phone', 2, false);
    searchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/kupit/kvartira/';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/kvartira/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('AMP. Офферов >= 3. Продажа. Не должен обрезать canonicalUrl.', () => {
    const data = getMockData('SELL', 'desktop', 100, true);
    searchSeoUrlsProvider.call(data, data);

    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/kvartira/st-ulica-cyurupy-55034/';
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('AMP. Офферов < 3. Продажа. Должен обрезать canonicalUrl. Остается только тип сделки и тип недвиги.', () => {
    const data = getMockData('SELL', 'desktop', 2, true);
    searchSeoUrlsProvider.call(data, data);

    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/kupit/kvartira/';
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('Десктоп. Офферов >= 3. Аренда. Не должен обрезать ampUrl и canonicalUrl.', () => {
    const data = getMockData('RENT', 'desktop', 100, false);
    searchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/snyat/kvartira/st-ulica-cyurupy-55034/';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/snyat/kvartira/st-ulica-cyurupy-55034/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

// eslint-disable-next-line max-len
it('Десктоп. Офферов < 3. Аренда. Должен обрезать ampUrl и canonicalUrl. Остается только тип сделки и тип недвиги.', () => {
    const data = getMockData('RENT', 'desktop', 2, false);
    searchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/snyat/kvartira/';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/snyat/kvartira/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('Тач. Офферов >= 3. Аренда. Не должен обрезать ampUrl и canonicalUrl.', () => {
    const data = getMockData('RENT', 'touch-phone', 100, false);
    searchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/snyat/kvartira/st-ulica-cyurupy-55034/';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/snyat/kvartira/st-ulica-cyurupy-55034/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('Тач. Офферов < 3. Аренда. Должен обрезать ampUrl и canonicalUrl. Остается только тип сделки и тип недвиги.', () => {
    const data = getMockData('RENT', 'touch-phone', 2, false);
    searchSeoUrlsProvider.call(data, data);

    const expectedAmpUrl = 'https://realty.yandex.ru/amp/moskva/snyat/kvartira/';
    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/snyat/kvartira/';

    expect(data.seo.ampUrl).toEqual(expectedAmpUrl);
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('AMP. Офферов >= 3. Аренда. Не должен обрезать canonicalUrl.', () => {
    const data = getMockData('RENT', 'desktop', 100, true);
    searchSeoUrlsProvider.call(data, data);

    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/snyat/kvartira/st-ulica-cyurupy-55034/';
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});

it('AMP. Офферов < 3. Аренда. Должен обрезать canonicalUrl. Остается только тип сделки и тип недвиги.', () => {
    const data = getMockData('RENT', 'desktop', 2, true);
    searchSeoUrlsProvider.call(data, data);

    const expectedCanonicalUrl = 'https://realty.yandex.ru/moskva/snyat/kvartira/';
    expect(data.seo.canonicalUrl).toEqual(expectedCanonicalUrl);
});
