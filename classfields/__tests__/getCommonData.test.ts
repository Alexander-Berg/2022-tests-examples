import { getCommonData } from '../getStructuredData';

it('Должен вернуть общую разметку', () => {
    const result = {
        '@context': 'https://schema.org',
        '@type': 'Organization',
        address: {
            '@type': 'PostalAddress',
            addressLocality: 'Москва',
            postalCode: '115035',
            streetAddress: 'Садовническая ул., 82, с2',
        },
        aggregateRating: {
            '@type': 'AggregateRating',
            bestRating: 5,
            ratingValue: 4.8,
            reviewCount: 115900,
        },
        logo: 'https://realty.yandex.ru/apple-touch-icon-192x192.png',
        name: 'Яндекс.Недвижимость',
        sameAs: ['https://vk.com/yandex.realty', 'https://ok.ru/realty.yandex'],
        telephone: '8 (800) 511-05-59',
        url: 'https://realty.yandex.ru',
    };

    expect(getCommonData()).toEqual(result);
});

it('Должен вернуть общую разметку без address', () => {
    const result = {
        '@context': 'https://schema.org',
        '@type': 'Organization',
        aggregateRating: {
            '@type': 'AggregateRating',
            bestRating: 5,
            ratingValue: 4.8,
            reviewCount: 115900,
        },
        logo: 'https://realty.yandex.ru/apple-touch-icon-192x192.png',
        name: 'Яндекс.Недвижимость',
        sameAs: ['https://vk.com/yandex.realty', 'https://ok.ru/realty.yandex'],
        telephone: '8 (800) 511-05-59',
        url: 'https://realty.yandex.ru',
    };

    expect(getCommonData(undefined, true)).toEqual(result);
});

it('Должен вернуть общую разметку без aggregateAppRating с флагом excludeAppRating = true', () => {
    const result = {
        '@context': 'https://schema.org',
        '@type': 'Organization',
        address: {
            '@type': 'PostalAddress',
            addressLocality: 'Москва',
            postalCode: '115035',
            streetAddress: 'Садовническая ул., 82, с2',
        },
        logo: 'https://realty.yandex.ru/apple-touch-icon-192x192.png',
        name: 'Яндекс.Недвижимость',
        sameAs: ['https://vk.com/yandex.realty', 'https://ok.ru/realty.yandex'],
        telephone: '8 (800) 511-05-59',
        url: 'https://realty.yandex.ru',
    };

    expect(getCommonData(true)).toMatchObject(result);
});
