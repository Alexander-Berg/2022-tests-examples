/**
 * @jest-environment node
 */
/* eslint-disable max-len */
import React from 'react';
import { shallow } from 'enzyme';

import { HelmetDefault } from 'core/client/components/Helmet/HelmetDefault/HelmetDefault';
import { HelmetTitle } from 'core/client/components/Helmet/HelmetTitle/HelmetTitle';
import { HelmetDescription } from 'core/client/components/Helmet/HelmetDescription/HelmetDescription';
import { mockStore } from 'core/mocks/store.mock';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { POST_FILTERS_SERVICE_MOCK_1 } from 'core/services/postFilters/mocks/postFiltersService.mock';
import {
    POST_LISTING_SERVICE_MOCK_1,
} from 'core/services/postListing/mocks/postListingService.mock';
import { ServiceId } from 'core/services/ServiceId';

import { PostListingHelmet } from './PostListingHelmet';

describe('рендерит все данные по-умолчанию', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST_FILTERS]: POST_FILTERS_SERVICE_MOCK_1,
        [ServiceId.POST_LISTING]: POST_LISTING_SERVICE_MOCK_1,
    });

    const wrapper = shallow(<PostListingHelmet/>);

    it('дефолтная разметка', () => {
        expect(wrapper.find(HelmetDefault).exists()).toBe(true);
    });

    it('title, description, canonical', () => {
        expect(wrapper.find(HelmetTitle).prop('title')).toBe('85 экспертных статей и исследований в разделе Учебник');
        expect(wrapper.find(HelmetDescription).prop('description')).toBe('Исследования рынка недвижимости и статьи - Учебник - 85 полезных статьи в Журнале Недвижимости');
        expect(wrapper.find('link[rel="canonical"]').prop('href')).toBe('https://realty.yandex.ru/journal/category/uchebnik/');
    });
});

