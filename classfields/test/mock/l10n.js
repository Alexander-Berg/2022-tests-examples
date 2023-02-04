var util = require('@vertis/nodules-libs').util,
    l10nMock = {
        'russia.spb' : {
            clientIp : '77.88.2.223',
            url : 'http://auto.yandex.ru',
            headers : {
                'accept-language' : 'en-US,en;q=0.8',
                'x-real-ip' : '77.88.2.223',
                'x-forwarded-for' : '77.88.2.223',
                host : 'auto.yandex.ru'
            }
        },
        'turkey.stambul' : {
            clientIp : '46.1.105.72',
            url : 'http://auto.yandex.com.tr',
            headers : {
                'accept-language' : 'en-US,en;q=0.8',
                'x-real-ip' : '46.1.105.72',
                'x-forwarded-for' : '46.1.105.72',
                host : 'auto.yandex.com.tr'
            }
        },
        'ukraine' : {
            clientIp : '5.248.255.255',
            url : 'http://auto.yandex.ua',
            headers : {
                'accept-language' : 'en-US,en;q=0.8',
                'x-real-ip' : '5.248.255.255',
                'x-forwarded-for' : '5.248.255.255',
                host : 'auto.yandex.ua'
            }
        },
        'belarus' : {
            clientIp : '87.252.227.102',
            // нет нац.домена
            url : 'http://auto.yandex.ru',
            headers : {
                'accept-language' : 'en-US,en;q=0.8',
                'x-real-ip' : '87.252.227.102',
                'x-forwarded-for' : '87.252.227.102',
                host : 'auto.yandex.ru'
            }
        }
    },
    extendMock = function(name, obj) {
        return util.extend({}, l10nMock[name], obj);
    };

l10nMock['russia.spb.overrides'] = extendMock('russia.spb', {
    url : 'http://auto.yandex.ru/?currency=TROLOLO&locale=OLOLO&lang=OLO' });

l10nMock['turkey.stambul.overrides'] = extendMock('turkey.stambul', {
    url : 'http://auto.yandex.com.tr/?currency=USD&locale=ru&lang=ru' });

module.exports = l10nMock;
