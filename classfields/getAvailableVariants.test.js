const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./getAvailableVariants');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('ошибка при неверном цвете', async() => {
    await expect(
        de.run(block, { context, params: { category: 'cars', section: 'all', color: 'FFFFFF' } }),
    ).rejects.toMatchObject({
        error: {
            id: 'BLOCK_GUARDED',
        },
    });
});

it('ошибка, если цвет undefined', async() => {
    await expect(
        de.run(block, { context, params: { category: 'cars', section: 'all', color: [ undefined ] } }),
    ).rejects.toMatchObject({
        error: {
            id: 'BLOCK_GUARDED',
        },
    });
});

it('200 при верном цвете', async() => {
    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .reply(200);

    await expect(
        de.run(block, { context, params: { category: 'cars', section: 'all', color: '97948F' } }),
    ).rejects.toMatchObject({
        error: {
            id: 'BLOCK_GUARDED',
        },
    });
});

it('200 при двух верных цветах', async() => {
    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .reply(200);

    await expect(
        de.run(block, { context, params: { category: 'cars', section: 'all', color: [ '97948F', '0000CC' ] } }),
    ).rejects.toMatchObject({
        error: {
            id: 'BLOCK_GUARDED',
        },
    });
});

it('200, если цвет это единственный элемент в массиве', async() => {
    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .reply(200);

    await expect(
        de.run(block, { context, params: { category: 'cars', section: 'all', color: [ '97948F' ] } }),
    ).rejects.toMatchObject({
        error: {
            id: 'BLOCK_GUARDED',
        },
    });
});

it('ошибка при неверном годе', async() => {
    await expect(
        de.run(block, { context, params: { category: 'cars', section: 'all', year_from: 1234, year_to: 1234 } }),
    ).rejects.toMatchObject({
        error: {
            id: 'BLOCK_GUARDED',
        },
    });
});

it('200 при верном годе', async() => {
    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .reply(200);

    await expect(
        de.run(block, { context, params: { category: 'cars', section: 'all', year_from: 2020, year_to: 2020 } }),
    ).rejects.toMatchObject({
        error: {
            id: 'BLOCK_GUARDED',
        },
    });
});

it('должен отработать guard, если не выполняется условие', async() => {
    await expect(
        de.run(block, { context, params: {
            section: 'all',
            category: 'cars',
            catalog_filter: [
                { mark: 'LEXUS' },
                { mark: 'CHERY' },
            ],
        } }),
    ).rejects.toMatchObject({
        error: {
            id: 'BLOCK_GUARDED',
        },
    });
});

it('ошибка, если неправильное количество сидений', async() => {
    await expect(
        de.run(block, { context, params: { category: 'cars', section: 'all', catalog_equipment: [ 'seats-111' ] } }),
    ).rejects.toMatchObject({
        error: {
            id: 'BLOCK_GUARDED',
        },
    });
});

it('ошибка, если неправильный объём двигателя', async() => {
    await expect(
        de.run(block, { context, params: { category: 'cars', section: 'all', displacement_from: 3001, displacement_to: 3001 } }),
    ).rejects.toMatchObject({
        error: {
            id: 'BLOCK_GUARDED',
        },
    });
});
