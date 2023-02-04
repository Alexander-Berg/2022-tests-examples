const { fillElement } = require('./helpers');
const { Roles } = require('../../../helpers/role_perm');

describe('admin', function () {
    describe('print-form-rule', function () {
        it('создание правила и валидация', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', 'print-form-rule.xml');
            await browser.ybWaitForLoad();

            const timestamp = Date.now();
            await browser.ybReplaceValue(
                '.yb-rule-attributes__externalId .yb-form-field__value',
                `id_${timestamp}`
            );
            await browser.ybReplaceValue(
                '.yb-rule-attributes__caption .yb-form-field__value',
                `пересекающееся правило`
            );
            await browser.ybLcomSelect('.yb-rule-attributes__type', 'Для договора');
            await browser.ybWaitForInvisible('.yb-rule-form_isBlocked');
            await browser.click('.yb-rule-links__add-rule-link');
            await browser.ybReplaceValue('.yb-rule-link__name-container', 'ссылочка 1');
            await browser.ybReplaceValue(
                '.yb-rule-link__value-container',
                'https://wiki.yandex-team.ru/woop'
            );
            await browser.click('.yb-rule-blocks__add-rule-block');

            await fillElement(
                browser,
                1,
                'Действующие условия договора',
                'Сервисы',
                'Равен',
                'Директ: Рекламные кампании'
            );
            await fillElement(
                browser,
                2,
                'Действующие условия договора',
                'Тип договора',
                'Равен',
                'Не агентский'
            );
            await fillElement(
                browser,
                3,
                'Действующие условия договора',
                'Фирма',
                'Равен',
                'ООО «Яндекс»'
            );
            await fillElement(
                browser,
                4,
                'Действующие условия договора',
                'Оплата',
                'Равен',
                'предоплата'
            );
            await browser.ybAssertView('заполненная форма', '.yb-content', {
                hideElements: ['.yb-rule-attributes__externalId .yb-form-field__value']
            });

            await browser.click('.yb-rule-form__save');
            await browser.waitForVisible('.Modal_visible .Modal-Content');
            await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');
            await browser.ybAssertView(
                'предупреждение о валидации',
                '.Modal_visible .Modal-Content'
            );

            await browser.click('.yb-intersections-confirmation__accept');
            await browser.waitForVisible('.yb-published-status');
            await browser.ybAssertView('просмотр правила', '.yb-content', {
                hideElements: ['.yb-rule-attributes__external-id .yb-form-field__value']
            });
        });

        it('создание, публикация и редактирование', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', 'print-form-rule.xml');
            await browser.ybWaitForLoad();

            const timestamp = Date.now();
            await browser.ybReplaceValue(
                '.yb-rule-attributes__externalId .yb-form-field__value',
                `id_${timestamp}`
            );
            await browser.ybReplaceValue(
                '.yb-rule-attributes__caption .yb-form-field__value',
                `непересекающееся правило`
            );
            await browser.ybLcomSelect('.yb-rule-attributes__contract-type', 'Расходный');
            await browser.ybWaitForInvisible('.yb-rule-form_isBlocked');
            await browser.click('.yb-rule-links__add-rule-link');
            await browser.ybReplaceValue('.yb-rule-link__name-container', 'ссылочка 1');
            await browser.ybReplaceValue(
                '.yb-rule-link__value-container',
                'https://wiki.yandex-team.ru/woop'
            );

            await browser.click('.yb-rule-blocks__add-rule-block');
            await fillElement(
                browser,
                1,
                'Действующие условия договора',
                'Фирма',
                'Равен',
                'Yandex Turkey'
            );
            await browser.click('.yb-rule-blocks__add-rule-block');
            await fillElement(
                browser,
                1,
                'Действующие условия договора',
                'Страна',
                'Равен',
                'Парагвай',
                2
            );

            await browser.click('.yb-rule-form__save');
            await browser.waitForVisible('.yb-published-status');
            await browser.ybAssertView('просмотр непересекающегося правила', '.yb-content', {
                hideElements: ['.yb-rule-attributes__external-id .yb-form-field__value']
            });

            await browser.click('.yb-rule-card__publish');
            await browser.ybWaitForInvisible('.yb-rule-card__publish');
            await browser.waitForVisible('.yb-published-status');
            await browser.ybAssertView('просмотр опубликованного правила', '.yb-content', {
                hideElements: ['.yb-rule-attributes__external-id .yb-form-field__value']
            });

            await browser.click('.yb-rule-card__edit');
            await browser.ybWaitForLoad();
            await browser.click(
                '.yb-rule-blocks__rule-blocks-container > div:nth-child(2) .yb-remove-button'
            );
            await browser.click('.yb-rule-form__save');
            await browser.ybWaitForLoad();
            await browser.ybAssertView('просмотр отредактированного правила', '.yb-content', {
                hideElements: [
                    '.yb-rule-attributes__external-id .yb-form-field__value',
                    '.yb-messages'
                ]
            });
        });
    });
});
