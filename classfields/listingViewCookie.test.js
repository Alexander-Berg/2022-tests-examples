const listingViewCookie = require('./listingViewCookie');

jest.mock('../lib/cookie');
const cookie = require('../lib/cookie');

const setCookieSpy = jest.spyOn(cookie, 'set');

describe('saveSession', function() {
    beforeEach(() => {
        cookie.get.mockImplementation(() => {});
    });

    it('если параметры не передавать, сохранит в куки дефолтные параметры', function() {
        listingViewCookie.saveSession();

        expect(setCookieSpy).toHaveBeenCalledTimes(2);
        expect(setCookieSpy).toHaveBeenNthCalledWith(1, 'listing_view_session', '{}');
        expect(setCookieSpy).toHaveBeenNthCalledWith(2, 'listing_view', '{"version":1}', { expires: 30 });
    });

    it('сохранит в куки переданные параметры', function() {
        listingViewCookie.saveSession({
            category: 'cars',
            section: 'new',
        });

        expect(setCookieSpy).toHaveBeenCalledTimes(2);
        expect(setCookieSpy).toHaveBeenNthCalledWith(1, 'listing_view_session', '{}');
        expect(setCookieSpy).toHaveBeenNthCalledWith(2, 'listing_view', '{"category":"cars","section":"new","version":1}', { expires: 30 });
    });

    it('добавит в параметры данные из кук', function() {
        cookie.get.mockImplementation((cookieName) => {
            switch (cookieName) {
                case 'listing_view':
                    return '{"price_from":10000}';
                case 'listing_view_session':
                    return '{"price_to":10000000}';
            }
            return '';
        });

        listingViewCookie.saveSession({});

        expect(setCookieSpy).toHaveBeenCalledTimes(2);
        expect(setCookieSpy).toHaveBeenNthCalledWith(1, 'listing_view_session', '{}');
        expect(setCookieSpy).toHaveBeenNthCalledWith(2, 'listing_view', '{"price_from":10000,"price_to":10000000,"version":1}', { expires: 30 });
    });

    it('перезапишет данные в таком порядке: общая кука, сессионная кука, переданные параметры', function() {
        cookie.get.mockImplementation((cookieName) => {
            switch (cookieName) {
                case 'listing_view':
                    return '{"price_from":10000,"price_to":2000000}';
                case 'listing_view_session':
                    return '{"price_from":20000,"year_from":1997}';
            }
            return '';
        });

        listingViewCookie.saveSession({ year_from: 2000 });

        expect(setCookieSpy).toHaveBeenCalledTimes(2);
        expect(setCookieSpy).toHaveBeenNthCalledWith(1, 'listing_view_session', '{}');
        expect(setCookieSpy).toHaveBeenNthCalledWith(
            2,
            'listing_view',
            '{"price_from":20000,"price_to":2000000,"year_from":2000,"version":1}',
            { expires: 30 },
        );
    });

    it('не сохраняет в куку listing_view параметры с дефолтными значениями', function() {
        listingViewCookie.saveSession({
            category: 'cars',
            currency: 'RUR',
            image: 'true',
            in_stock: 'true',
            section: 'new',
        });

        expect(setCookieSpy).toHaveBeenCalledTimes(2);
        expect(setCookieSpy).toHaveBeenNthCalledWith(1, 'listing_view_session', '{}');
        expect(setCookieSpy).toHaveBeenNthCalledWith(2, 'listing_view', '{"category":"cars","in_stock":"true","section":"new","version":1}', { expires: 30 });
    });

});
