/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { HomeHelmet } from 'core/client/features/Home/HomeHelmet/HomeHelmet';
import { mockStore } from 'core/mocks/store.mock';
import { ServiceId } from 'core/services/ServiceId';
import { HOME_SERVICE_MOCK_1 } from 'core/services/home/mocks/homeService.mock';
import { ListingBlocks } from 'core/client/features/ListingBlocks/ListingBlocks';

import { HomeListingsLinks } from '../HomeListingsLinks/HomeListingsLinks';

import HomeMain from './HomeMain';
import { HomeNavigationTags } from './HomeNavigationTags/HomeNavigationTags';

it('в разметке присутствует все обязательные элементы для страницы', () => {
    mockStore({
        [ServiceId.HOME]: HOME_SERVICE_MOCK_1,
    });

    const wrapper = shallow(<HomeMain/>);

    expect(wrapper.find(HomeHelmet).exists()).toBe(true);
    expect(wrapper.find(ListingBlocks).exists()).toBe(true);
    expect(wrapper.find(HomeListingsLinks).exists()).toBe(true);
    expect(wrapper.find(HomeNavigationTags).exists()).toBe(true);
});
