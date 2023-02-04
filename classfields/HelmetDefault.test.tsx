/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { mockStore } from 'core/mocks/store.mock';
import { ServiceId } from 'core/services/ServiceId';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { APP_NAME } from 'core/client/constants/appName';

import { HelmetDefault } from './HelmetDefault';

describe('рендерит все дефолтные теги', () => {
    mockStore({ [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const wrapper = shallow(
        <HelmetDefault/>
    );

    it('мета og:site_name"', () => {
        expect(wrapper.find('meta[property="og:site_name"]').prop('content')).toBe(APP_NAME);
    });

    it('мета og:type', () => {
        expect(wrapper.find('meta[property="og:type"]').prop('content')).toBe('website');
    });

    it('мета og:locale', () => {
        expect(wrapper.find('meta[property="og:locale"]').prop('content')).toBe('ru_RU');
    });

    it('мета og:title', () => {
        expect(wrapper.find('meta[property="og:title"]').prop('content')).toBe(APP_NAME);
    });

    it('мета og:url', () => {
        expect(wrapper.find('meta[property="og:url"]').prop('content')).toBe(ROUTER_SERVICE_MOCK_1.data?.fullUrl);
    });

    it('каноникал', () => {
        expect(wrapper.find('link[rel="canonical"]').prop('href')).toBe(ROUTER_SERVICE_MOCK_1.data?.fullUrl);
    });

    it('amphtml', () => {
        expect(wrapper.find('link[rel="amphtml"]').prop('href')).toBe(
            '/amp/journal/post/testovaya-statya-so-vsemi-blokami-dlya-testirovaniya/'
        );
    });
});
