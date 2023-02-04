import { getListener } from '../helpers';
import { mockWindow } from '../mock-generators';

import { getEventMock } from './get-event-mock';

export class OpenerMock {
    private isInstalled: boolean = false;
    private spy: jest.SpyInstance | null = null;

    install() {
        if (this.isInstalled) {
            throw new Error('OpenerMock: already installed');
        }

        mockWindow('top.opener');
        this.spy = jest.spyOn(window, 'addEventListener');
        this.isInstalled = true;
    }

    uninstall() {
        if (!this.isInstalled) {
            throwNotInstalled();
        }

        this.isInstalled = false;
        this.spy.mockRestore();
        this.spy = null;
    }

    triggerEvent(eventName: string, data: object) {
        if (!this.isInstalled) {
            throwNotInstalled();
        }

        getListener(
            this.spy,
            'message',
        )({
            origin: 'localhost',
            data: getEventMock(eventName, 'foobar-order-id', data),
        });
    }
}

export function getMock(): OpenerMock {
    return new OpenerMock();
}

function throwNotInstalled() {
    throw new Error('OpenerMock: not installed');
}
