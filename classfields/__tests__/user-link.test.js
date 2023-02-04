import React from 'react';
import { mount, createGateMock, createStoreMock } from 'view/libs/test-helpers';
import { UserLink } from '..';

const storeMock = createStoreMock({});

const gateMock = createGateMock({
    getClients() {
        return Promise.resolve({ clients: [] });
    }
});

describe('user link', () => {
    it('should not render link for moderator', async() => {
        const ctx = {
            gate: gateMock,
            store: storeMock.extend({ user: { permissions: [] } })
        };
        const { dom, time } = mount(<UserLink uid='123' name='Locky' />, ctx);

        await time.tick();
        expect(dom.text()).toBe('Locky');
        expect(dom.find('a')).toHaveLength(0);
    });

    it('should render link for admin', async() => {
        const ctx = {
            gate: gateMock,
            store: storeMock.extend({ user: { permissions: [ 'read_and_write_user_clients' ] } })
        };
        const { dom, time, router } = mount(<UserLink uid='123' name='Locky' />, ctx);

        await time.tick();
        expect(dom.text()).toBe('Locky');
        expect(dom.find('a').prop('href')).toBe(router.createHref({ page: 'user', params: { userId: '123' } }));
    });
});
