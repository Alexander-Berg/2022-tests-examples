const listingUrlBuilder = require('./listingUrlsBuilder');
const { DEMO_PARAMS, MOTO_DEMO_PARAMS } = require('../lib/constants');
const generateCombinationsCache = require('www-url/app/lib/generateCombinationsCache');

const combinationsCache = generateCombinationsCache();

it('возвращает правильное количество ссылок для урлов с гео и без гео', () => {
    expect(listingUrlBuilder(DEMO_PARAMS, combinationsCache)).toHaveLength(1024);
});

it('возвращает правильное количество ссылок для урлов с гео', () => {
    const params = {
        ...DEMO_PARAMS,
        exclude_no_geo_url: true,
    };
    expect(listingUrlBuilder(params, combinationsCache)).toHaveLength(508);
});

it('возвращает правильное количество ссылок для мото и комтранса', () => {
    expect(listingUrlBuilder(MOTO_DEMO_PARAMS, combinationsCache)).toHaveLength(25);
});
