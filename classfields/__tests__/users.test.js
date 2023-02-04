import React from 'react';
import { formatNumber } from 'view/libs/number';
import { createBackendMock, mount } from 'view/libs/test-helpers';
import { UsersPage } from '..';

const routerMock = {
    entries: [
        { page: 'users', params: {} }
    ]
};

const backend = createBackendMock({
    'cream.get_users'() {
        return Promise.resolve({
            users: [
                { uid: '1', name: 'Иванов Иван' },
                { uid: '2', name: 'Сидоров Антон' },
                { uid: '3', name: 'Константинопольский Константин Константинович' }
            ]
        });
    },
    'cream.get_user_clients'({ userId }) {
        return Promise.resolve({
            [1]: { userId: '1' },
            [2]: { userId: '2', clientId: [] },
            [3]: { userId: '3', clientId: [ '10', '20', '30' ] }
        }[userId]);
    }
});

describe('user clients page', () => {
    it('should render empty page', async() => {
        const ctx = {
            router: routerMock,
            backend: backend.extend({
                'cream.get_users'() {
                    return Promise.resolve({ users: [] });
                }
            })
        };

        const { dom, gate } = mount(<UsersPage />, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('Нет менеджеров');
    });

    it('should render error message', async() => {
        const ctx = {
            router: routerMock,
            backend: backend.extend({
                'cream.get_users'() {
                    return Promise.reject();
                }
            })
        };

        const { dom, gate } = mount(<UsersPage />, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('Непредвиденная ошибка');
    });

    it('should render managers', async() => {
        const ctx = {
            router: routerMock,
            backend
        };

        const { dom, gate } = mount(<UsersPage />, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('Иванов Иван');
        expect(dom.text()).toContain('Сидоров Антон');
        expect(dom.text()).toContain('Константинопольский Константин Константинович');
        expect(dom.text()).toContain('0');
        expect(dom.text()).toContain('3');
    });

    it('should rerender on route change', async() => {
        let firstRequestCompleted = false;

        const ctx = {
            router: routerMock,
            backend: backend.extend({
                'cream.get_users'() {
                    const promise = Promise.resolve({
                        users: [
                            { uid: '1', name: 'Иванов Иван' },
                            firstRequestCompleted && { uid: '2', name: 'Сидоров Антон' }
                        ].filter(Boolean)
                    });

                    firstRequestCompleted = true;
                    return promise;
                }
            })
        };

        const { dom, gate, router } = mount(<UsersPage />, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('Иванов Иван');

        router.push({ page: 'main', params: {} });
        router.push({ page: 'users', params: {} });

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain('Иванов Иван');
        expect(dom.text()).toContain('Сидоров Антон');
    });

    it('should format clients count', async() => {
        const ctx = {
            router: routerMock,
            backend: backend.extend({
                'cream.get_user_clients'() {
                    return Promise.resolve({
                        clientId: { length: 10000 }
                    });
                }
            })
        };

        const { dom, gate } = mount(<UsersPage />, ctx);

        await gate.waitForPendingRequests();
        expect(dom.text()).toContain(formatNumber(10000));
    });
});
