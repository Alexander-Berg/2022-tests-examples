import { intervalWatcherNodes } from '../interval-watcher-nodes';
import { mutationObserverWatcher } from '../mutation-observer-watcher';

const wait = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(() => resolve(), ms));

describe('dom-watcher', () => {
    test('interval-watcher-nodes iframe only', async () => {
        const checkedIframe: string[] = [];

        intervalWatcherNodes(['iframe'], (node: HTMLIFrameElement) => {
            checkedIframe.push(node.src);
        });

        const iframe = document.createElement('iframe');
        iframe.src = 'https://yandex.ru/';

        document.body.appendChild(iframe);

        await wait(1000);

        expect(checkedIframe).toContain('https://yandex.ru/');
    });

    test('mutation-observer-watcher iframe with nesting', async () => {
        const checkedIframe: string[] = [];

        mutationObserverWatcher(['iframe'], (node: HTMLIFrameElement) => {
            checkedIframe.push(node.src);
        });

        const iframe = document.createElement('iframe');
        iframe.src = 'https://yandex.ru/';

        const firstDiv = document.createElement('div');
        const secondDiv = document.createElement('div');

        firstDiv.appendChild(iframe);
        secondDiv.appendChild(firstDiv);

        document.body.appendChild(secondDiv);

        await wait(1000);

        expect(checkedIframe).toContain('https://yandex.ru/');
    });

    test('mutation-observer-watcher iframe only', async () => {
        const checkedIframe: string[] = [];

        mutationObserverWatcher(['iframe'], (node: HTMLIFrameElement) => {
            checkedIframe.push(node.src);
        });

        const iframe = document.createElement('iframe');
        iframe.src = 'https://yandex.ru/';

        document.body.appendChild(iframe);

        await wait(0);

        expect(checkedIframe).toContain('https://yandex.ru/');
    });

    test('interval-watcher-nodes iframe with nesting', async () => {
        const checkedIframe: string[] = [];

        mutationObserverWatcher(['iframe'], (node: HTMLIFrameElement) => {
            checkedIframe.push(node.src);
        });

        const iframe = document.createElement('iframe');
        iframe.src = 'https://yandex.ru/';

        const firstDiv = document.createElement('div');
        const secondDiv = document.createElement('div');

        firstDiv.appendChild(iframe);
        secondDiv.appendChild(firstDiv);

        document.body.appendChild(secondDiv);

        await wait(0);

        expect(checkedIframe).toContain('https://yandex.ru/');
    });
});
