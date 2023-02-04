const getBreadcrumbNameBySearchParams = require('./getBreadcrumbNameBySearchParams');
const stateMock = require('../../../mocks/ampCatalogStore.mock');

const breadcrumbs = stateMock.breadcrumbsPublicApi.data;

it('должен вернуть объект с правильными категориями для mark', () => {
    expect(getBreadcrumbNameBySearchParams('MARK_LEVEL', 'kia', breadcrumbs)).toEqual('Kia');
});

it('должен вернуть объект с правильными категориями для model', () => {
    expect(getBreadcrumbNameBySearchParams('MODEL_LEVEL', 'rio', breadcrumbs)).toEqual('Rio');
});

it('должен вернуть объект с правильными категориями для super_gen', () => {
    expect(getBreadcrumbNameBySearchParams('GENERATION_LEVEL', '22500704', breadcrumbs)).toEqual('IV Рестайлинг');
});

it('должен вернуть объект с правильными категориями для configuration_id', () => {
    expect(getBreadcrumbNameBySearchParams('CONFIGURATION_LEVEL', '22500752', breadcrumbs)).toEqual('Седан');
});

it('должен вернуть пустую строку, когда нет нужных данных', () => {
    expect(getBreadcrumbNameBySearchParams('Hello', 'World', [])).toEqual('');
});

it('должен вернуть пустую строку, когда не передаем никаких данных', () => {
    expect(getBreadcrumbNameBySearchParams()).toEqual('');
});
