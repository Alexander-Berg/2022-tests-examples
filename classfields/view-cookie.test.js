const viewCookie = require('./view-cookie');

describe('parseCookie', function() {
    it('парсит куку', function() {
        const cookie = viewCookie.parseCookie('{"output_type":"carousel","version":1}', '');

        expect(cookie).toEqual({
            output_type: 'carousel',
            version: 1,
        });
    });

    it('возвращает пустой объект, если кука невалидная', function() {
        const cookie = viewCookie.parseCookie('{"output_type":"carousel', '');

        expect(cookie).toEqual({});
    });

    it('возвращает пустой объект, если кука пустая', function() {
        const cookie = viewCookie.parseCookie(undefined, '');

        expect(cookie).toEqual({});
    });

    it('не учитывает сортировку из куки', function() {
        const cookie = viewCookie.parseCookie('{"sort_offers":"cr_date-DESC"}', '');

        expect(cookie).toEqual({});
    });

    it('не учитывает сортировку из сессионной куки', function() {
        const cookie = viewCookie.parseCookie('', '{"sort_offers":"cr_date-DESC"}');

        expect(cookie).toEqual({});
    });

    it('обогащает результат из сессионной куки', function() {
        const cookie = viewCookie.parseCookie('{"output_type":"carousel","version":1}', '{"version":2}');

        expect(cookie).toEqual({
            output_type: 'carousel',
            version: 2,
        });
    });
});
