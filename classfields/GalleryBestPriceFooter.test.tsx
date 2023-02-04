import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import GalleryBestPriceFooter from './GalleryBestPriceFooter';

const store = mockStore({
    matchApplication: {},
    user: { data: {} },
});

it('правильно формируется тайтл в зависимости от оффера', () => {
    const wrapper = shallow(
        <Provider store={ store }>
            <GalleryBestPriceFooter
                offer={ offerMock }
            />
        </Provider>,
        { context: { ...contextMock, store } },
    ).dive().dive();

    const title = wrapper.find('.GalleryBestPriceFooter__mainText').text();
    expect(title).toEqual('Ford EcoSport по лучшей цене.');
});
