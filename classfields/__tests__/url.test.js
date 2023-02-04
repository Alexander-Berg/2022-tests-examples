const ServiceUrl = require('../url');
const router = require('realty-router');

const Url = new ServiceUrl({
    url: 'https://realty.yandex.ru',
    routers: router,
    viewType: 'desktop'
});

describe('Построение canonical на разводящей странице', () => {
    it('Не строим ссылку для amp ', () => {
        const url = Url.getSeoUrl({
            params: {
                rgidCode: 'tambovskaya_oblast',
                rgid: 54970,
                type: 'RENT'
            },
            urlType: 'amp',
            type: 'search-categories'
        });

        expect(url).toBeNull();
    });

    it('Строим каноникл в зависимости от параметров', () => {
        const url = Url.getSeoUrl({
            params: {
                rgidCode: 'tambovskaya_oblast',
                rgid: 54970,
                type: 'RENT'
            },
            urlType: 'canonical',
            type: 'search-categories'
        });

        expect(url).toEqual('https://realty.yandex.ru/1-e_otd_sovhoza_bondarskiy/snyat/');
    });
});
