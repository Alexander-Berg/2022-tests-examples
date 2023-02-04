import { pageFocusWatcher } from '../page-focus-watcher';

const { visibilityState } = document;

const wait = (ms: number) => new Promise((r) => setTimeout(r, ms));

describe('page-focus-watcher', function () {
    afterEach(() => {
        // @ts-ignore see _config/jest/setup-after-env.js
        document.visibilityState = visibilityState;
    });

    test('should trigger by undefined visibilityState', async function () {
        const callback = jest.fn();

        // @ts-ignore see _config/jest/setup-after-env.js
        document.visibilityState = undefined;

        pageFocusWatcher(callback, 100);

        await wait(200);

        expect(callback).toBeCalledTimes(1);
        expect(callback).toBeCalledWith('undefined');
    });

    test('should trigger by timer', async function () {
        const callback = jest.fn();

        // @ts-ignore see _config/jest/setup-after-env.js
        document.visibilityState = 'hidden';

        pageFocusWatcher(callback, 100);

        await wait(200);

        expect(callback).toBeCalledTimes(1);
        expect(callback).toBeCalledWith('timer', 'hidden');
    });

    test('should trigger by visible', async function () {
        const callback = jest.fn();

        // @ts-ignore see _config/jest/setup-after-env.js
        document.visibilityState = 'hidden';

        pageFocusWatcher(callback, 800);

        await wait(400);

        // @ts-ignore see _config/jest/setup-after-env.js
        document.visibilityState = 'visible';

        await wait(500);

        expect(callback).toBeCalledTimes(1);
        expect(callback).toBeCalledWith();
    });
});
