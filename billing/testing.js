module.exports = {
    static: {
        yandexHost: '//betastatic.yastatic-net.ru',
    },

    blackbox: {
        yandex: {
            host: 'blackbox-mimino.yandex.net',
        },
    },

    developer: {
        api: {
            host: 'https://apikeys-test.paysys.yandex.net:8668',
        },
        surveys: {
            host: 'https://forms.test.yandex.ru/surveys/',
        },
        balance: {
            host: 'https://user-balance.greed-ts.paysys.yandex.ru',
        },
    },

    maps: {
        constructor: {
            ru: {
                url: '//constructor-front01g.tst.maps.yandex.ru',
            },
            com: {
                url: '//constructor-front01g.tst.maps.yandex.com',
            },
            'com.tr': {
                url: '//constructor-front01g.tst.maps.yandex.com.tr',
            },
        },
        sandbox: {
            host: '//tst.sandbox.api.maps.yandex.ru',
            strictSSL: false,
        },
    },

    ca: {
        path: '/etc/ssl/certs/YandexInternalRootCA.pem',
    },
};
