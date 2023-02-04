import breadcrumbsPublicApi from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';

import getMmmMultiItemInfo from './getMmmMultiItemInfo';

it('должен правильно отдавать MmmMultiItemInfo', () => {
    const item = { mark: 'AUDI', model: 'A4', nameplate: '456', generation: '123', exclude: false };

    expect(getMmmMultiItemInfo(item, { breadcrumbsPublicApi })).toMatchSnapshot();
});
