import listing from 'autoru-frontend/mockData/state/listing';

import catalogTechParam from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import seo from './electroCard';

it('должен правильно сформировать сео', () => {
    const state = {
        catalogTechParam,
        listing,
    };

    const pageParams = {
        category: 'cars',
        mark: 'audi',
        model: 'e_tron',
        super_gen: '21447469',
        configuration_id: '21447519',
        tech_param_id: '22146045',
    };

    expect(seo(state, pageParams)).toMatchSnapshot();
});
