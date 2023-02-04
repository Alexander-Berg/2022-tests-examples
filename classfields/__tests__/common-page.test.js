import React from 'react';
import { createBackendMock, mount } from 'view/libs/test-helpers';
import { UserPage } from '../common/page';

const router = {
    entries: [
        { page: 'userClients', params: { userId: '123' } }
    ]
};

const backend = createBackendMock({
    'cream.get_user'() {
        return Promise.resolve({ uid: '123', name: 'Locky' });
    },
    'cream.get_user_clients'({ userId }) {
        return Promise.resolve({ userId, clientId: [] });
    },
    'cream.get_client_name'({ clientId }) {
        return Promise.resolve({ clientId, name: `Tor-${clientId}` });
    }
});

describe('common client page', () => {
    it('should render page content', async() => {
        const ctx = {
            router,
            backend
        };

        const { dom, gate } = mount(<UserPage>page content</UserPage>, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('page content');
    });

    it('should render user name', async() => {
        const ctx = {
            router,
            backend
        };

        const { dom, gate } = mount(<UserPage>page content</UserPage>, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('Locky');
    });

    it('should render user without name', async() => {
        const ctx = {
            router,
            backend: backend.extend({
                'cream.get_user'() {
                    return Promise.resolve({ uid: '123', name: '' });
                }
            })
        };

        const { dom, gate } = mount(<UserPage>page content</UserPage>, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('ID 123');
        expect(dom.text()).toContain('page content');
    });

    it('should render not found page', async() => {
        const ctx = {
            router,
            backend: backend.extend({
                'cream.get_user'() {
                    return Promise.reject({ code: 'HTTP_NOT_FOUND' });
                }
            })
        };

        const { dom, gate } = mount(<UserPage>page content</UserPage>, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('Менеджер с ID 123 не найден');
        expect(dom.text()).not.toContain('page content');
    });

    it('should render 403 page', async() => {
        const ctx = {
            router,
            backend: backend.extend({}, { permissions: [] })
        };

        const { dom, gate } = mount(<UserPage>page content</UserPage>, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('Нет прав для просмотра страницы');
        expect(dom.text()).not.toContain('page content');
    });

    it('should render unknown error', async() => {
        const ctx = {
            router,
            backend: backend.extend({
                'cream.get_user'() {
                    return Promise.reject();
                }
            })
        };

        const { dom, gate } = mount(<UserPage>page content</UserPage>, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('Неизвестная ошибка');
        expect(dom.text()).not.toContain('page content');
    });

    it('should show clients count', async() => {
        const ctx = {
            router,
            backend: backend.extend({
                'cream.get_user_clients'({ userId }) {
                    return Promise.resolve({ userId, clientId: [ '1', '2', '3' ] });
                }
            })
        };

        const { dom, gate } = mount(<UserPage>page content</UserPage>, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('Клиенты3');
    });

    // TODO: добавить тест, что количество клиентов выводится на других страницах, когда эти страницы появятся
});
