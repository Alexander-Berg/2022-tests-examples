const geo = require('./geo');

jest.mock('auto-core/react/lib/cookie', () => {
    return {
        getCsrfToken: () => 'csrf-token',
    };
});

describe('redirect', () => {
    let originLocation;
    beforeEach(() => {
        originLocation = global.location;

        global.location = { href: 'http://localhost' };
    });

    afterEach(() => {
        global.location = originLocation;
    });

    it('должен подставить текущий href, если ничего не передали', () => {
        geo.redirect({ geo: '213' });

        expect(location.href).toEqual('https://autoru_frontend.base_domain/georedir/?geo=213&url=http%3A%2F%2Flocalhost&_csrf_token=csrf-token');
    });

    it('должен подставить для редиректа переданный url', () => {
        geo.redirect({ geo: '213', url: 'https://auto.ru' });

        expect(location.href).toEqual('https://autoru_frontend.base_domain/georedir/?geo=213&url=https%3A%2F%2Fauto.ru&_csrf_token=csrf-token');
    });

    it('должен стереть гео, если передали пустой массив', () => {
        geo.redirect({ geo: [], url: 'https://auto.ru' });

        expect(location.href).toEqual('https://autoru_frontend.base_domain/georedir/?geo=&url=https%3A%2F%2Fauto.ru&_csrf_token=csrf-token');
    });
});
