/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { mockStore } from 'core/mocks/store.mock';
import { ServiceId } from 'core/services/ServiceId';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { POST_SERVICE_MOCK_1 } from 'core/services/post/mocks/postService.mock';
import { HelmetDefault } from 'core/client/components/Helmet/HelmetDefault/HelmetDefault';
import { HelmetDescription } from 'core/client/components/Helmet/HelmetDescription/HelmetDescription';

import { PostHelmet } from './PostHelmet';

describe('рендерит все данные по-умолчанию', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST]: POST_SERVICE_MOCK_1,
    });

    const wrapper = shallow(<PostHelmet/>);
    const helmetWrapper = wrapper.find('HelmetWrapper');

    it('дефолтная разметка', () => {
        expect(wrapper.find(HelmetDefault).exists()).toBe(true);
    });

    it('title, description, canonical', () => {
        expect(helmetWrapper.find('title').exists()).toBe(true);
        expect(helmetWrapper.find('link[rel="canonical"]').prop('href')).toBe(ROUTER_SERVICE_MOCK_1.data?.fullUrl);
        expect(wrapper.find(HelmetDescription).exists()).toBe(true);
    });

    it('правильное количество meta тегов', () => {
        expect(helmetWrapper.find('meta')).toHaveLength(7);
    });

    it('LDJson разметку Article', () => {
        const LDJson = JSON.parse(helmetWrapper.find('script[type="application/ld+json"]').text());

        expect(LDJson['@type']).toBe('Article');
    });
});

