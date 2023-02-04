import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import mockStore from 'autoru-frontend/mocks/mockStore';
import cardMock from 'autoru-frontend/mockData/state/groupCard.mock';
import hideMock from 'autoru-frontend/mockData/state/helpers/hideMock';
import contextMock from 'autoru-frontend/mocks/contextMock';

const mockAdditionalModelsPromise = Promise.resolve([ cardMock, cardMock ]);
jest.mock('auto-core/react/dataDomain/autoguru/helpers/getAdditionalModels', () => () => mockAdditionalModelsPromise);

import AutoGuruAdditionalModels from './AutoGuruAdditionalModels';

const state = {
    listing: {
        data: {
            offers: [ cardMock, cardMock ],
            search_parameters: {
                currency: 'RUR',
            },
        },
        search_id: '777',
    },
    autoguru: {
        questions: [],
        answerValues: [],
    },
};

const store = mockStore(state);

it('должен получить список дополнительных моделей и вывести его', () => {
    const tree = shallow(
        <AutoGuruAdditionalModels excludedOffers={ [] }/>,
        { context: { ...contextMock, store } },
    ).dive();

    return mockAdditionalModelsPromise
        .then(() => {
            expect(shallowToJson(tree, { map: hideMock(cardMock, 'offer', '[Offer mock]') })).toMatchSnapshot();
        });
});
