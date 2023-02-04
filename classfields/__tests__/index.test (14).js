import React from 'react';
import * as enzyme from 'enzyme';

import * as h from './common';

describe('ClientMenu', () => {
    it('all links have correct hrefs', () => {
        const wrapper = enzyme.mount(<h.Component links={h.links} mock={h.defaultStoreMock} />);

        expect(wrapper.find(`[href='${h.correctSelfLkLink}']`).last().text()).toBe('ЛК');
        expect(wrapper.find(`[href='${h.links.feeds}']`).last().text()).toBe('Фиды');
        expect(wrapper.find(`[href='${h.links.verba}']`).last().text()).toBe('Верба');
        expect(wrapper.find(`[href='${h.links.pd}']`).last().text()).toBe('Pipedrive');
        expect(wrapper.find(`[href='${h.links.moderation}']`).last().text()).toBe('Модерилка');
        expect(wrapper.find(`[href='${h.links.balance}']`).last().text()).toBe('Баланс');
    });

    it('getting id for lk href from adAgency', () => {
        const mock = {
            ...h.defaultStoreMock,
            client: {
                ...h.defaultStoreMock.client,
                common: { data: { id: 'bil_1337' } }
            }
        };

        const wrapper = enzyme.mount(<h.Component links={h.links} mock={mock} />);

        expect(wrapper.find(`[href='${h.correctAdAgencyLkLink}']`).last().text()).toBe('ЛК');
    });
});
