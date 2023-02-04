/**
 * @jest-environment jsdom
 */

import Dispatcher from '../../../Dispatcher';
import Client from '../Client';

describe('Basic tests of Client', () => {
    it('Subscribes to window "message" event', (done) => {
        const dispatcher = new Dispatcher();
        const client = new Client({ trustedOrigins: '*' }, dispatcher);

        dispatcher.subscribe(Dispatcher.EVENT.CLIENT.ADD_ITEM, () => {
            done();
        });

        client.subscribe();
        window.postMessage({ data: { service_id: 1, service_order_id: 1 } }, '*');
    });

    it('Sends data message', (done) => {
        const dispatcher = new Dispatcher();
        const client = new Client({ trustedOrigins: '*' }, dispatcher);

        const data = { data: 1 };
        const fn = (event) => {
            window.parent.removeEventListener('message', fn);
            expect(event.data).toEqual({ data, version: 1 });
            done();
        };
        window.parent.addEventListener('message', fn);
        client.sendMessage(data);
    });

    it('Sends error message', (done) => {
        const dispatcher = new Dispatcher();
        const client = new Client({ trustedOrigins: '*' }, dispatcher);

        const error = { code: 1, message: 'message' };
        const fn = (event) => {
            window.parent.removeEventListener('message', fn);
            expect(event.data).toEqual({ error, version: 1 });
            done();
        };
        window.parent.addEventListener('message', fn);
        client.sendErrorMessage(error);
    });
});

describe('Check trusted origins', () => {
    const trustedOrigins = {
        'https://admin-balance.greed-tm.paysys.yandex.ru': true,
        'https://admin-balance.greed-ts.paysys.yandex.ru': true,
        'https://local-cart.yandex.ru': true,
        'https://direct.yandex.ru': true,
        '*.direct.yandex.ru': true
    };

    it('Should be checked correctly', () => {
        expect(
            Client.isTrustedOrigin(
                'https://admin-balance.greed-tm.paysys.yandex.ru',
                trustedOrigins
            )
        ).toBeTruthy();
        expect(
            Client.isTrustedOrigin('https://direct.yandex.ru', trustedOrigins)
        ).toBeTruthy();
        expect(
            Client.isTrustedOrigin(
                'https://some-test.direct.yandex.ru',
                trustedOrigins
            )
        ).toBeTruthy();
        expect(
            Client.isTrustedOrigin('https://test.direct.yandex.ru', trustedOrigins)
        ).toBeTruthy();
        expect(
            Client.isTrustedOrigin(
                'https://test.someother.yandex.ru',
                trustedOrigins
            )
        ).toBeFalsy();
    });
});
