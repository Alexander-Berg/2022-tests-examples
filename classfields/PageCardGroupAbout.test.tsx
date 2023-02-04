import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { cardGroupComplectations } from 'autoru-frontend/mockData/state/cardGroupComplectations.mock';
import listing from 'autoru-frontend/mockData/state/listing.js';

import PageCardGroupAbout from './PageCardGroupAbout';

const catalogFilter = [ {
    mark: 'Ford',
    model: 'EcoSport',
    generation: '20104320',
    configuration: '20104322',
} ];

const store = mockStore({
    cardGroupComplectations,
    catalogConfigurationsSubtree: {},
    config: { data: { pageParams: { catalog_filter: catalogFilter } } },
    listing,
});

it('PageCardGroupAbout должен отправить метрику view', () => {
    shallow(<PageCardGroupAbout/>, { context: { ...contextMock, store } }).dive();

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'about_model', 'about', 'view' ]);
});
