const assert = require('chai').assert;

const { hideElements, ignoreElements, personModalSelector } = require('./helpers');

describe('admin', function () {
    describe('common', function () {
        it('просмотр заархивированного и не заархивированного плательщика', async function () {
            const { browser } = this;

            await browser.ybSetSteps(
                `Получает id клиента с плательщиками: заархивированным и не заархивированным`
            );
            const [clientId, , externalIdArchived, , externalIdNotArchived] = await browser.ybRun(
                'test_hidden_and_not_hidden_person_act'
            );

            await browser.ybSignIn({ isAdmin: true });

            await browser.ybUrl('admin', 'acts.xml');

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps('Заполняет акт, клиента');
            await browser.ybReplaceValue(
                '.yb-acts-search__external-id',
                String(externalIdArchived)
            );

            await browser.click('.yb-acts-search__client .Textinput');

            await browser.ybReplaceValue('.yb-clients-search__client-id', String(clientId));

            await browser.ybFilterDoModalSearch();

            await browser.waitForVisible('.yb-clients-table__select-client');
            await browser.click('.yb-clients-table__select-client');

            await browser.ybFilterDoSearch();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(`Открывает модалку плательщика`);
            await browser.click('.yb-acts-table__person-name');

            await browser.ybSetSteps(`Дожидается появления содержимого в модалке`);
            await browser.waitForVisible(personModalSelector);
            await browser.ybWaitForInvisible(personModalSelector + ' .yb-fetching');

            await browser.ybAssertView('archived заархивированный', personModalSelector, {
                hideElements,
                ignoreElements
            });

            await browser.ybSetSteps('Закрывает модалку');
            await browser.click('.Modal_visible #modal-close-btn-null');

            await browser.ybSetSteps('Заполняет акт, клиента');
            await browser.ybReplaceValue(
                '.yb-acts-search__external-id',
                String(externalIdNotArchived)
            );

            await browser.click('.yb-acts-search__client .Textinput');

            await browser.ybReplaceValue('.yb-clients-search__client-id', String(clientId));

            await browser.ybFilterDoModalSearch();

            await browser.waitForVisible('.yb-clients-table__select-client');
            await browser.click('.yb-clients-table__select-client');

            await browser.ybSetSteps(`Ищет акты`);
            await browser.ybFilterDoSearch();

            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybSetSteps(`Открывает модалку плательщика`);
            await browser.click('.yb-acts-table__person-name');

            await browser.ybSetSteps(`Дожидается появления содержимого в модалке`);
            await browser.waitForVisible(personModalSelector);
            await browser.ybWaitForInvisible(personModalSelector + ' .yb-fetching');

            await browser.ybAssertView('archived не заархивированный', personModalSelector, {
                hideElements,
                ignoreElements
            });
        });
    });
});
