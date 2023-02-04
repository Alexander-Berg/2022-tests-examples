import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockImage from 'autoru-frontend/mockData/images/320';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { Dealer } from 'auto-core/react/dataDomain/dealersListing/types';
import ButtonWithLoader from 'auto-core/react/components/islands/ButtonWithLoader/ButtonWithLoader';

import EmptyGroupDealersItem from './EmptyGroupDealersItem';

const dealerMock = {
    dealerName: 'Major Expert Сокольники',
    phones: {
        list: [],
    },
    address: 'Московская область, городской округ Красногорск, Пятницкое шоссе, 6-й километр, 3, м. Пятницкое шоссе, Митино',
    dealerCode: 'Major',
    dealerId: 123,
    dealerLink: 'foo.bar',
    dealerLogo: mockImage,
} as unknown as Dealer;

const Context = createContextProvider(contextMock);

it('должен отправить метрику при клике на телефон', async() => {
    const wrapper = shallow(
        <Context>
            <EmptyGroupDealersItem
                dealer={ dealerMock }
                phonesList={ [] }
                onPhoneClick={ jest.fn() }
            />
        </Context>,
    ).dive();

    wrapper.find(ButtonWithLoader).simulate('click');

    expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PHONE_NEW_CL_CARS2');
});
