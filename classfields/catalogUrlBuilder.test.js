const catalogUrlBuilder = require('./catalogUrlBuilder');
const { CATALOG_DEMO_PARAMS } = require('../lib/constants');

it('Возвращает все каталожные урлы для сайтмапа', () => {
    expect(catalogUrlBuilder(CATALOG_DEMO_PARAMS)).toMatchSnapshot();
});
