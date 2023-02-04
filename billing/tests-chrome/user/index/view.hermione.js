const { hideElements } = require('./helpers');
const assert = require('chai').assert;

describe('user', () => {
    describe('index', () => {
        it('нет привязанного клиента', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yb-static-balance-5' });
            await browser.ybUrl('user', 'index.xml');

            await browser.ybWaitForLoad();

            await browser.ybAssertView('главная страница, не привязан клиент', '.yb-new-layout', {
                hideElements
            });
        });

        it('привязан пустой клиент + ссылки', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            await browser.ybRun('create_client_for_user', { login });
            await browser.ybUrl('user', 'index.xml');

            await browser.ybWaitForLoad();
            await browser.ybAssertView('главная страница, привязан клиент', '.yb-new-layout', {
                hideElements
            });

            await browser.ybAssertLink(`.yb-user-nav__item_orders`, `orders.xml`);
            await browser.ybAssertLink(`.yb-user-nav__item_invoices`, `invoices.xml`);
            await browser.ybAssertLink(`.yb-user-nav__item_acts`, `acts.xml`);
            await browser.ybAssertLink(`.yb-user-nav__item_settlements`, `settlements.xml`);
            await browser.ybAssertLink(
                `.yb-user-nav__item_support`,
                `https://yandex.ru/support/balance/`,
                { isAccurate: true }
            );
        });

        it('запрос пин-кода', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            await browser.ybRun('create_client_for_user', { login });
            await browser.ybUrl('user', 'index.xml');
            await browser.ybWaitForLoad();

            await browser.click('.yb-user-header-content__account');
            await browser.ybAssertView('меню паспорта', 'body', {
                hideElements: [...hideElements, '.yb-passport-item__name']
            });

            await browser.click('.yb-passport-item_pin-code');
            await browser.waitForVisible('.yb-user-pin-code__modal');
            await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');
            await browser.ybAssertView('пинкод', 'body', {
                hideElements: [...hideElements, '.yb-user-pin-code__code']
            });

            await browser.click('.yb-user-pin-code__code-button button');
            await browser.ybAssertView(
                'главная страница, закрыта модалка с пинкодом',
                '.yb-new-layout',
                { hideElements }
            );
        });
    });
});
