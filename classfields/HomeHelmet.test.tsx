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
import { ServiceId } from 'core/services/ServiceId';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';

import { HomeHelmet } from './HomeHelmet';

describe('рендерит все данные по-умолчанию', () => {
    mockStore({ [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const wrapper = shallow(<HomeHelmet/>);

    it('дефолтная разметка', () => {
        expect(wrapper.find(HelmetDefault).exists()).toBe(true);
    });

    it('title', () => {
        expect(wrapper.find(HelmetTitle).prop('title')).toBe('Исследования на рынке недвижимости, интервью экспертов и статьи про квартиры, новостройки и дома в Журнале Недвижимости');
    });

    it('ogTitle', () => {
        expect(wrapper.find(HelmetTitle).prop('ogTitle')).toBe('Я так живу');
    });

    it('description', () => {
        expect(wrapper.find(HelmetDescription).prop('description')).toBe('Лучшие экспертные статьи про покупку и продажу квартир и домов, основные тенденции рынка недвижимости, документы для сделок и другое в Журнале Яндекс.Недвижимости');
    });

    it('canonical', () => {
        expect(wrapper.find('link[rel="canonical"]').prop('href')).toBe('https://realty.yandex.ru/journal/');
    });
});

