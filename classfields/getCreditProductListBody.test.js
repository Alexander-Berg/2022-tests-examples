const getCreditProductListBody = require('./getCreditProductListBody');
const requestTypes = require('auto-core/react/dataDomain/credit/dicts/productListRequestType');

it('вернет верный body для запроса по гео', () => {
    expect(getCreditProductListBody({
        type: 'BY_GEO',
        data: { geobaseIDs: [ 1, 2, 3 ] },
    })).toEqual({ by_geo: { geobase_ids: [ 1, 2, 3 ] } });
});

it('вернет верный body для запроса по кредитной заявке', () => {
    expect(getCreditProductListBody({
        type: requestTypes.BY_CREDIT_APPLICATION,
        data: { creditApplicationID: '123-456' },
    })).toEqual({ by_credit_application: { credit_application_id: '123-456' } });
});

it('вернет верный body для запроса всех кредитных продуктов', () => {
    expect(getCreditProductListBody({ type: requestTypes.ALL })).toEqual({ all: {} });
});

it('вернет body запроса всех кредитных продуктов, если нет geobaseIDs', () => {
    expect(getCreditProductListBody({ type: requestTypes.BY_GEO })).toEqual({ all: {} });
});

it('вернет body запроса всех кредитных продуктов, если нет creditApplicationID', () => {
    expect(getCreditProductListBody({ type: requestTypes.BY_CREDIT_APPLICATION })).toEqual({ all: {} });
});

it('вернет добавит неактивные продукты с параметром', () => {
    expect(getCreditProductListBody({ type: requestTypes.BY_CREDIT_APPLICATION, withInactive: true }))
        .toEqual({ all: {}, withInactive: true });
});
