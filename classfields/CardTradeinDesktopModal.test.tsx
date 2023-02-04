import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import mockOffer from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import { nbsp } from 'auto-core/react/lib/html-entities';

import CardTradeinDesktopModal from './CardTradeinDesktopModal';

const store = mockStore({ tradein: { tradeinPrice: { data: 613000 } } });

it('должен брать цену из предикта', async() => {
    const wrapper = shallow(
        <CardTradeinDesktopModal
            userOffers={ [ mockOffer ] }
            offer={ mockOffer }
            onSubmit={ _.noop }
            onRequestHide={ _.noop }
            visible
            isMobile={ false }
        />, { context: { ...contextMock, store } },
    ).dive();

    expect(wrapper.find('.CardTradeinDesktopModal__priceInfoItemPrice').at(1).text()).toBe(`~ 613${ nbsp }000${ nbsp }₽`);
});
