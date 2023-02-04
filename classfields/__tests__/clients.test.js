import React from 'react';
import Spinner from 'vertis-react/components/Spinner';
import { createGateMock, mount } from 'view/libs/test-helpers';
import { UserClientsPage } from '../clients';

const router = {
    entries: [
        { page: 'userClients', params: { userId: '123' } }
    ]
};

const gateMock = createGateMock({
    getUser() {
        return Promise.resolve({ name: 'Locky' });
    },
    getUserClients() {
        return Promise.resolve({ clients: [] });
    },
    deleteClient() {
        return Promise.resolve({ status: 'ok' });
    },
    addClient() {
        return Promise.resolve({ status: 'ok' });
    }
});

describe('user clients page', () => {
    it('should render empty page', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                getUserClients() {
                    return Promise.resolve({ clients: [] });
                }
            })
        };

        const { dom, gate } = mount(<UserClientsPage />, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('0 клиентов');
    });

    it('should render page with client', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                getUserClients() {
                    return Promise.resolve({ clients: [ { name: 'Locky', clientId: '123' } ] });
                }
            })
        };

        const { dom, gate } = mount(<UserClientsPage />, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('1 клиент');
        expect(dom.text()).toContain('Locky');
    });

    it('should pluralize title', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                getUserClients() {
                    return Promise.resolve({
                        clients: [ { name: 'Locky', clientId: '123' }, { name: 'Tor', clientId: '124' } ]
                    });
                }
            })
        };

        const { dom, gate } = mount(<UserClientsPage />, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('2 клиента');
    });

    it('should handle unexpected error', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                getUserClients() {
                    return Promise.reject({ errorCode: 'UNEXPECTED' });
                }
            })
        };

        const { dom, gate } = mount(<UserClientsPage />, ctx);

        await gate.waitForPendingRequests();

        expect(dom.text()).toContain('Неизвестная ошибка');
        expect(dom.text()).not.toContain('Привязать клиента');
    });

    it('should remove client', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                getUserClients() {
                    return Promise.resolve({ clients: [ { name: 'Locky', clientId: '123' } ] });
                }
            })
        };

        const { dom, gate } = mount(<UserClientsPage />, ctx);

        await gate.waitForPendingRequests();

        dom.find('button[data-test="remove-client-button"]').simulate('click');
        expect(dom.find(Spinner).length).toBe(1);

        await gate.waitForPendingRequests();

        expect(dom.text()).toContain('Удалено');
        expect(dom.text()).toContain('Вернуть');
    });

    it('should restore client', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                getUserClients() {
                    return Promise.resolve({ clients: [ { name: 'Locky', clientId: '123' } ] });
                }
            })
        };

        const { dom, gate } = mount(<UserClientsPage />, ctx);

        await gate.waitForPendingRequests();

        const htmlBeforeRemoval = dom.html();

        dom.find('button[data-test="remove-client-button"]').simulate('click');

        await gate.waitForPendingRequests();

        dom.find('button[data-test="restore-client-button"]').simulate('click');

        await gate.waitForPendingRequests();

        expect(dom.html()).toBe(htmlBeforeRemoval);
    });

    it('should show error after unsuccessful removing', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                getUserClients() {
                    return Promise.resolve({ clients: [ { name: 'Locky', clientId: '123' } ] });
                },
                deleteClient() {
                    return Promise.reject({ errorCode: 'UNEXPECTED' });
                }
            })
        };

        const { dom, gate } = mount(<UserClientsPage />, ctx);

        await gate.waitForPendingRequests();

        dom.find('button[data-test="remove-client-button"]').simulate('click');

        await gate.waitForPendingRequests();

        expect(dom.text()).toContain('Ошибка');
        expect(dom.text()).not.toContain('Удалено');
        expect(dom.text()).not.toContain('Вернуть');
    });

    it('should show error after unsuccessful restoring', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                getUserClients() {
                    return Promise.resolve({ clients: [ { name: 'Locky', clientId: '123' } ] });
                },
                addClient() {
                    return Promise.reject({ errorCode: 'UNEXPECTED' });
                }
            })
        };

        const { dom, gate } = mount(<UserClientsPage />, ctx);

        await gate.waitForPendingRequests();

        dom.find('button[data-test="remove-client-button"]').simulate('click');

        await gate.waitForPendingRequests();

        dom.find('button[data-test="restore-client-button"]').simulate('click');

        await gate.waitForPendingRequests();

        expect(dom.text()).toContain('Ошибка');
        expect(dom.text()).toContain('Удалено');
        expect(dom.text()).toContain('Вернуть');
    });
});

describe('user connecting', () => {
    it('should show label when placeholder is hidden', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                searchClients() {
                    const clients = [
                        { name: 'Locky', clientId: '123' },
                        { name: 'Loco Bedoya', clientId: '456' }
                    ];

                    return Promise.resolve({ clients });
                }
            })
        };

        const { dom, gate } = mount(<UserClientsPage />, ctx);
        const root = () => dom.find('[data-test="clients-list"]').first();
        const suggest = () => dom.find('[data-test="add-user-suggest"]').first();

        await gate.waitForPendingRequests();

        suggest().find('input').simulate('focus');
        suggest().find('input').simulate('change', { target: { value: 'Loc' } });

        await gate.waitForPendingRequests();

        expect(root().find('input').instance().value).toBe('Loc');
        expect(suggest().find('label').text()).toContain('Найти и привязать клиента');
    });

    it('should show users list', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                searchClients() {
                    const clients = [
                        { name: 'Locky', clientId: '123' },
                        { name: 'Loco Bedoya', clientId: '456' }
                    ];

                    return Promise.resolve({ clients });
                }
            })
        };

        const { dom, gate, time } = mount(<UserClientsPage />, ctx);
        const suggest = () => dom.find('[data-test="add-user-suggest"]').first();

        await gate.waitForPendingRequests();

        suggest().find('input').simulate('focus');
        suggest().find('input').simulate('change', { target: { value: 'Loc' } });

        await gate.waitForPendingRequests();
        await time.tick(300);

        expect(suggest().find('ul').text()).toContain('Locky');
        expect(suggest().find('ul').text()).toContain('Loco Bedoya');
    });

    it('should add client after click on suggest item', async() => {
        const ctx = {
            router,
            gate: gateMock.extend({
                searchClients() {
                    const clients = [
                        { name: 'Locky', clientId: '123' },
                        { name: 'Loco Bedoya', clientId: '456' }
                    ];

                    return Promise.resolve({ clients });
                }
            })
        };

        const { dom, gate, time } = mount(<UserClientsPage />, ctx);
        const root = () => dom.find('[data-test="clients-list"]').first();
        const suggest = () => dom.find('[data-test="add-user-suggest"]').first();

        await gate.waitForPendingRequests();

        suggest().find('input').simulate('focus');
        suggest().find('input').simulate('change', { target: { value: 'Loc' } });

        await gate.waitForPendingRequests();
        await time.tick(300);

        expect(root().find('table').text()).not.toContain('Locky');

        suggest().findWhere(e => {
            return e.get(0) && e.get(0).type === 'li' && e.text().includes('Locky');
        }).simulate('mousedown');

        expect(root().find('table').text()).toContain('Locky');
        expect(root().find('table').find(Spinner)).toHaveLength(1);
        await gate.waitForPendingRequests();

        expect(root().find('table').find(Spinner)).toHaveLength(0);
    });
});
