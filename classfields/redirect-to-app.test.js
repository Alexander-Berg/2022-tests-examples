const middleware = require('./redirect-to-app');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    req.method = 'GET';
    req.uatraits.isMobile = true;
    req.router.route.getName.mockImplementation(() => 'react-form-add');
});

it('не должен сделать редирект для десктопа', () => {
    expect.assertions(1);

    req.uatraits.isMobile = false;
    middleware(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('не должен сделать редирект для 404', () => {
    expect.assertions(1);

    req.router = null;
    middleware(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('не должен сделать редирект для POST-запроса', () => {
    expect.assertions(1);

    req.method = 'POST';
    middleware(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('должен средиректить мобилку со страницы добавления объявления в промку аппа', () => {
    expect.assertions(1);

    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'MOBILE_FORM_TO_APP',
            data: {
                location: 'https://autoru_frontend.base_domain/promo/from-web-to-app/',
                status: 302,
            },
        });
    });
});

it('должен средиректить мобилку со страницы редактирования объявления в промку аппа и не потерять гет-параметр', () => {
    expect.assertions(1);

    req.router.route.getName.mockImplementation(() => 'react-form-edit');
    req.router.params = {
        utm_content: 'some-content',
        from: 'some-where',
    };
    middleware(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'MOBILE_FORM_TO_APP',
            data: {
                location: 'https://autoru_frontend.base_domain/promo/from-web-to-app/?utm_content=some-content&from=some-where',
                status: 302,
            },
        });
    });
});
