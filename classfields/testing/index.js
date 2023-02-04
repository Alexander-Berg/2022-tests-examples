const resources = require('./resources');

module.exports = Object.assign(
    {
        env: 'testing',
        pass: {
            url: 'https://pass-test.yandex.ru'
        },
        passport: {
            url: 'https://passport-test.yandex.ru'
        },
        passportApi: {
            url: 'https://api.passport-test.yandex.ru'
        },
        apiServerHost: 'https://realty.test.vertis.yandex.ru/api/1.0',
        partnerUrl: 'https://partner.realty.test.vertis.yandex.ru',

        moderationServiceHost: 'moderation.test.vertis.yandex-team.ru',

        ugcTestingUid: '971923671',

        yapicHost: 'yapic-test.yandex.ru',
        avatarsHost: 'avatars.mdst.yandex.net',

        yandexKassaV3ShopId: 601234,

        yaArendaUrl: 'https://arenda.test.vertis.yandex.ru/',

        yaArendaAgencyId: 4062678234,

        realtyUrl: 'https://realty.test.vertis.yandex.ru/',

        creamUrl: 'https://cream.test.vertis.yandex-team.ru'
    },
    resources);
