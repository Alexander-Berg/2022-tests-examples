const _ = require('lodash');

const filtersMock = require('www-cabinet/react/dataDomain/calls/mocks/withFilters.mock').filters;

const getPreparedFilters = require('./getPreparedFilters');

it('должен правильно маппить фильтры для запроса в бэк', () => {
    const filters = _.cloneDeep(filtersMock);

    const result = getPreparedFilters(filters);

    expect(result).toMatchSnapshot();
});

it('должен правильно маппить фильтры с range значениями, если нет одного из параметров', () => {
    const filters = _.cloneDeep(filtersMock);
    filters.year_from = undefined;
    filters.price_from = undefined;

    const result = getPreparedFilters(filters);

    expect(_.pick(result, [ 'year', 'price' ])).toMatchSnapshot();
});

it('должен отдавать пустой объект, если нет фильтров', () => {
    const result = getPreparedFilters({});

    expect(result).toMatchSnapshot();
});
