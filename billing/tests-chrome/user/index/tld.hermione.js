const assert = require('chai').assert;
const { hideElements } = require('./helpers');

describe('user', () => {
    describe('index', () => {
        it('com домен', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({}, { tld: 'com' });
            const { client_id } = await browser.ybRun('create_client_for_user', { login });
            await browser.ybUrl('user', 'index.xml', { tld: 'com' });

            await browser.ybWaitForLoad();

            await browser.ybAssertView('главная страница, на домене .com', '.yb-new-layout', {
                hideElements: [...hideElements, '.yb-user-pic__own-name']
            });

            const realLogin = await browser.getText('.yb-user-pic__own-name');
            assert(login, realLogin, 'Логины не совпадают');
        });

        it('com.tr домен', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({}, { tld: 'com.tr' });
            const { client_id } = await browser.ybRun('create_client_for_user', { login });
            await browser.ybUrl('user', 'index.xml', { tld: 'com.tr' });

            await browser.ybWaitForLoad();

            await browser.ybAssertView('главная страница, на домене .com.tr', '.yb-new-layout', {
                hideElements: [...hideElements, '.yb-user-pic__own-name']
            });

            const realLogin = await browser.getText('.yb-user-pic__own-name');
            assert(login, realLogin, 'Логины не совпадают');
        });

        it('kz домен', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({}, { tld: 'kz' });
            const { client_id } = await browser.ybRun('create_client_for_user', { login });
            await browser.ybUrl('user', 'index.xml', { tld: 'kz' });

            await browser.ybWaitForLoad();

            await browser.ybAssertView('главная страница, на домене .kz', '.yb-new-layout', {
                hideElements: [...hideElements, '.yb-user-pic__own-name']
            });

            const realLogin = await browser.getText('.yb-user-pic__own-name');
            assert(login, realLogin, 'Логины не совпадают');
        });
    });
});
