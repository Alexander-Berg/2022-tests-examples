jest.mock('auto-core/lib/util/getBunkerDict');
const getBunkerDict = require('auto-core/lib/util/getBunkerDict');
getBunkerDict.mockImplementation(() => new Promise(resolve => resolve([ 'approvedwebsite.com' ])));

jest.mock('auto-core/lib/util/isAutoruUrl');
const isAutoruUrl = require('auto-core/lib/util/isAutoruUrl');
isAutoruUrl.mockImplementation((url) => url === 'https://auto.ru');

const isEmbedAllowed = require('./isEmbedAllowed');

it('должен вернуть true, если referer относится к урлам Авто.ру', async() => {
    const result = await isEmbedAllowed(null, 'https://auto.ru');

    expect(result).toBe(true);
});

it('должен вернуть true, если роут widget-banner и referer в banners/whitelist', async() => {
    const result = await isEmbedAllowed('widget-banner', 'https://approvedwebsite.com');

    expect(result).toBe(true);
});

it('должен вернуть false, если роут widget-banner и referer не в banners/whitelist', async() => {
    const result = await isEmbedAllowed('widget-banner', 'https://deniedwebsite.com');

    expect(result).toBe(false);
});

it('должен вернуть false, если роут не widget-banner и referer в banners/whitelist', async() => {
    const result = await isEmbedAllowed('fuel_calculator', 'https://approvedwebsite.com');

    expect(result).toBe(false);
});

describe('route "panorama-admin"', () => {
    it('возвращает true если зашли с вертикального хоста', async() => {
        const result = await isEmbedAllowed('panorama-admin', 'https://autocenter.test.vertis.yandex-team.ru/offers/view/1102031128-0acbc358/panorama');

        expect(result).toBe(true);
    });

    it('возвращает true если зашли с локального хоста', async() => {
        const result = await isEmbedAllowed('panorama-admin', 'https://localhost/offers/view/1102031128-0acbc358/panorama');

        expect(result).toBe(true);
    });

    it('возвращает true если зашли с рандомного хоста', async() => {
        const result = await isEmbedAllowed('panorama-admin', 'https://google.ru');

        expect(result).toBe(false);
    });
});
