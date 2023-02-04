const assert = require('chai').assert;

describe('user', () => {
    describe('acts', () => {
        describe('ecopy', () => {
            it('get проверяет получение электронной копии акта', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn();
                await browser.ybRun('create_client_with_act', [login]);

                await browser.ybUrl('user', 'acts.xml');

                await browser.ybSetSteps(`Кликает в акт
Вводит почту и что-то неправильное в поле капчи`);

                // FF bug workaround  *[type="submit"]
                await browser.waitForVisible('*[name="external_id"]');
                await browser.waitForVisible('#ecopies-get-form .b-payments__td [type="checkbox"]');
                await browser.waitForVisible('*[name="ecopy-email"]');

                await browser.click('#ecopies-get-form .b-payments__td [type="checkbox"]');
                await browser.setValue('*[name="ecopy-email"]', 'vasya@yandex.ru');
                // FF bug workaround  body #id
                await browser.setValue('body #ecopy-captcha-rep', '012345x'); // Точно неправильный текст

                await browser.ybAssertView('ecopy get filled', '.content', {
                    ignoreElements: ['body #ecopy-captcha-image', '.captcha-image', '.b-payments']
                });

                await browser.ybSetSteps(`Запрашивает акты
Ждет появления окна`);

                await browser.click('body #ecopy-button');

                await browser.waitUntil(async function () {
                    const style = await browser.getAttribute('.fancybox-wrap', 'style');
                    return /opacity: 1/.test(style);
                });

                await browser.ybAssertView('ecopy get fancybox', '.content', {
                    ignoreElements: ['body #ecopy-captcha-image', '.captcha-image', '.b-payments']
                });

                await browser.ybSetSteps(`Нажимает запросить
Дожидается всплывающего окна с ошибкой неправильной капчи`);

                await browser.click('.fancybox-overlay #getCopySubmitButton');
            });
        });
    });
});
