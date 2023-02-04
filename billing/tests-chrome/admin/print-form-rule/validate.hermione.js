const { Roles } = require('../../../helpers/role_perm');

describe('admin', function () {
    describe('print-form-rule', function () {
        it('проверка ошибок', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', 'print-form-rule.xml');
            await browser.ybWaitForLoad();
            await browser.click('.yb-rule-form__save');

            await browser.ybAssertView('пустая форма с ошибками', '.yb-content');

            await browser.ybReplaceValue(
                '.yb-rule-attributes__externalId .yb-form-field__value',
                'невалидный идентификатор'
            );
            await browser.click('.yb-rule-links__add-rule-link');
            await browser.ybReplaceValue('.yb-rule-link__value-container', 'i am not a link');
            await browser.click('.yb-rule-form__save');

            await browser.ybAssertView('непустая форма с ошибками', '.yb-content');

            await browser.ybReplaceValue('.yb-rule-link__name-container', 'ссылочка 1');
            await browser.ybReplaceValue(
                '.yb-rule-link__value-container',
                'https://wiki.yandex-team.ru/woop'
            );
            await browser.click('.yb-rule-links__add-rule-link');
            await browser.ybReplaceValue(
                '.yb-rule-link:nth-child(2) .yb-rule-link__name-container',
                'ссылочка 2'
            );
            await browser.ybReplaceValue(
                '.yb-rule-link:nth-child(2) .yb-rule-link__value-container',
                'https://wiki.yandex-team.ru/woop'
            );
            await browser.click('.yb-rule-form__save');

            await browser.ybAssertView('одинаковые ссылки', '.yb-rule-links__container');
        });
    });
});
