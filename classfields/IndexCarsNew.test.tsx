import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import IndexCarsNew from './IndexCarsNew';

const defaultState = {
    indexCarsNew: {
        offers: [],
    },
    listing: {
        searchId: 'azazaz',
    },
};

const storeMock = mockStore(defaultState);

it('не будет рендерить блок, если нет эксперимента', () => {
    const tree = shallow(
        <IndexCarsNew/>,
        { context: { ...contextMock, store: storeMock } },
    ).dive();

    expect(tree).toBeEmptyRender();
});

it('не будет рендерить блок, если офферов нет', () => {
    const context = {
        ...contextMock,
        hasExperiment: (exp: string) => exp === 'AUTORUFRONT-20709_new-auction',
    };
    const tree = shallow(
        <IndexCarsNew/>,
        { context: { ...context, store: storeMock } },
    ).dive();

    expect(tree).toBeEmptyRender();
});
