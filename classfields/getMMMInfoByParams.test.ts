import breadcrumbsPublicApi from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';

import getMMMInfoByParams from './getMMMInfoByParams';

it('правильно отдает MMMInfo по Params при пустом фильтре', () => {
    expect(getMMMInfoByParams({}, { breadcrumbsPublicApi })).toBeNull();
});

it('правильно отдает MMMInfo по Params при заданной марке', () => {
    const params = {
        mark: 'FORD',
    };

    expect(getMMMInfoByParams(params, { breadcrumbsPublicApi })).toMatchSnapshot();
});

it('правильно отдает MMMInfo по Params при заданной марке, модели', () => {
    const params = {
        mark: 'FORD',
        model: 'ECOSPORT',
    };

    expect(getMMMInfoByParams(params, { breadcrumbsPublicApi })).toMatchSnapshot();
});

it('правильно отдает MMMInfo по Params при заданной марке, модели, поколении', () => {
    const params = {
        mark: 'FORD',
        model: 'ECOSPORT',
        super_gen: '20104320',
    };

    expect(getMMMInfoByParams(params, { breadcrumbsPublicApi })).toMatchSnapshot();
});
