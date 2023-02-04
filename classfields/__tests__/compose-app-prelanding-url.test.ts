import composeUrl from '../compose-app-prelanding-url';

const COMMON_PARAMS = {
    utm_campaign: 'utm_campaign',
    utm_content: 'utm_content',
    utm_term: 'utm_term',
    fallback: 'https://yandex.ru',
};

describe('composeAppPrelandingUrl', () => {
    it('Корректно строит URL с относительным deepLink', () => {
        expect(
            composeUrl(
                'android',
                {
                    ...COMMON_PARAMS,
                    deep_link: '/some-path',
                },
                true
            )
        ).toBe(
            'https://bzfk.adj.st/some-path?adjust_t=wsudr1e_e8tc29m&adjust_campaign=utm_campaign&adjust_adgroup=utm_content&adjust_creative=utm_term&adjust_fallback=https%3A%2F%2Fyandex.ru&adjust_deeplink=yandexrealty%3A%2F%2Frealty.yandex.ru%2Fsome-path'
        );
    });

    it('Корректно строит URL с относительным deepLink без / в начале', () => {
        expect(
            composeUrl(
                'android',
                {
                    ...COMMON_PARAMS,
                    deep_link: 'some-path',
                },
                true
            )
        ).toBe(
            'https://bzfk.adj.st/some-path?adjust_t=wsudr1e_e8tc29m&adjust_campaign=utm_campaign&adjust_adgroup=utm_content&adjust_creative=utm_term&adjust_fallback=https%3A%2F%2Fyandex.ru&adjust_deeplink=yandexrealty%3A%2F%2Frealty.yandex.ru%2Fsome-path'
        );
    });

    it('Корректно строит URL с абсолютным deepLink', () => {
        expect(
            composeUrl(
                'android',
                {
                    ...COMMON_PARAMS,
                    deep_link: 'https://realty.yandex.ru/some-path',
                },
                true
            )
        ).toBe(
            'https://bzfk.adj.st/?adjust_t=wsudr1e_e8tc29m&adjust_campaign=utm_campaign&adjust_adgroup=utm_content&adjust_creative=utm_term&adjust_fallback=https%3A%2F%2Fyandex.ru&adjust_deeplink=yandexrealty%3A%2F%2Frealty.yandex.ru%2Fsome-path'
        );
    });
});
