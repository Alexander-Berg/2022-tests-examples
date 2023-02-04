const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const block = require('./convert-listing-url-to-embed-carousel-url');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен вернуть 404 для пустого урла', () => {
    return de.run(block, { context }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toEqual({
                error: {
                    id: 'UNKNOWN_URL_TO_CONVERT',
                    status_code: 404,
                },
            });
        });
});

it('должен вернуть 404 для неизвестный урлов', () => {
    context.req.url = 'https%3A%2F%2Fauto.ru%2Fthis-is-404-route%2F';
    return de.run(block, { context }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toEqual({
                error: {
                    id: 'UNKNOWN_URL_TO_CONVERT',
                    status_code: 404,
                },
            });
        });
});

it('должен вернуть 404 если ссылка не на листинг', () => {
    context.req.url = 'https%3A%2F%2Fauto.ru%2Fdilery%2Fcars%2Fall%2F';
    return de.run(block, { context }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toEqual({
                error: {
                    id: 'UNKNOWN_URL_TO_CONVERT',
                    status_code: 404,
                },
            });
        });
});

it('должен вернуть параметры поиска для листинга с одной маркой-моделью', () => {
    return de.run(block, {
        context,
        params: {
            // eslint-disable-next-line max-len
            url: 'https%3A%2F%2Fauto.ru%2Fcars%2Faudi%2Fa6%2F21210593%2Fall%2F%3Fcatalog_equipment%3Dautomatic-lighting-control%26catalog_equipment%3Dlight-cleaner%26catalog_equipment%3Dptf%26catalog_equipment%3Dhigh-beam-assist',
        },
    })
        .then(() => {
            expect(res.send).toHaveBeenCalledWith({
                // eslint-disable-next-line max-len
                url: 'https://promo.test.avto.ru/embed/listing-carousel/?category=cars&section=all&catalog_equipment=automatic-lighting-control&catalog_equipment=light-cleaner&catalog_equipment=ptf&catalog_equipment=high-beam-assist&catalog_filter=mark%3DAUDI%2Cmodel%3DA6%2Cgeneration%3D21210593',
            });
        });
});

it('должен вернуть параметры поиска для листинга с несколькими марками-моделями', () => {
    return de.run(block, {
        context,
        params: {
            // eslint-disable-next-line max-len
            url: 'https%3A%2F%2Fauto.ru%2Fcars%2Fall%2F%3Fcatalog_equipment%3Dautomatic-lighting-control%26catalog_equipment%3Dlight-cleaner%26catalog_equipment%3Dptf%26catalog_equipment%3Dhigh-beam-assist%26catalog_filter%3Dmark%253DAUDI%252Cmodel%253DA7%252Cgeneration%253D21134030%26catalog_filter%3Dmark%253DAUDI%252Cmodel%253DA8%252Cgeneration%253D21040120',
        },
    })
        .then(() => {
            expect(res.send).toHaveBeenCalledWith({
                // eslint-disable-next-line max-len
                url: 'https://promo.test.avto.ru/embed/listing-carousel/?category=cars&section=all&catalog_equipment=automatic-lighting-control&catalog_equipment=light-cleaner&catalog_equipment=ptf&catalog_equipment=high-beam-assist&catalog_filter=mark%3DAUDI%2Cmodel%3DA7%2Cgeneration%3D21134030&catalog_filter=mark%3DAUDI%2Cmodel%3DA8%2Cgeneration%3D21040120',
            });
        });
});

it('должен вернуть параметры поиска для бесшильдовой модификации', () => {
    return de.run(block, {
        context,
        params: {
            url: 'https://auto.ru/moskva/cars/volkswagen/passat-passat/6391671/all/',
        },
    })
        .then(() => {
            expect(res.send).toHaveBeenCalledWith({
                // eslint-disable-next-line max-len
                url: 'https://promo.test.avto.ru/embed/listing-carousel/?category=cars&section=all&catalog_filter=mark%3DVOLKSWAGEN%2Cmodel%3DPASSAT%2Cnameplate_name%3D--%2Cgeneration%3D6391671',
            });
        });
});
