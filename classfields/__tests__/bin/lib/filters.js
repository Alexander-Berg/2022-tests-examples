const FILTERS = require('realty-router/lib/filters/meta');

describe('FILTERS', () => {
    it('должен возвращать настройки для формирования ЧПУ на фильтрах', () => {
        expect(FILTERS).toMatchSnapshot();
    });
});
