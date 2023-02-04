const assert = require('chai').assert;

const elements = {
    list: '.yb-client-page__client-data .yb-page-section table',
    editLink: '.yb-client-page__client-data .yb-page-section tbody tr:first-child td:nth-child(6) a'
};

describe('admin', () => {
    describe('aliases', () => {
        it('нет алиасов, проверяет ссылку на редактирование', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });
            const { client_id } = await browser.ybRun('create_client_for_user', [login]);
            await browser.ybUrl('admin', `aliases.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(elements.list);
            await browser.ybAssertView('клиент без алиасов', elements.list);

            const expectedHref = `/editclient.xml?tcl_id=${client_id}&agency_id=`;
            await browser.ybSetSteps(
                `Проверяет, что ссылка на редактирование содержит '${expectedHref}'`
            );
            const editHref = await browser.getAttribute(elements.editLink, 'href');
            assert(editHref.indexOf(expectedHref) >= 0, 'некорректная ссылка на редактирование');
        });

        it('есть алиасы', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });
            const { client_id } = await browser.ybRun('create_client_with_aliases_for_user', [
                login
            ]);
            await browser.ybUrl('admin', `aliases.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(elements.list);
            await browser.ybAssertView('клиент с алиасами', elements.list);

            await browser.ybAssertLink(
                'a=Никифор I',
                `tclient.xml?tcl_id=${client_id}&retpath=https%3A%2F%2Fadmin-balance.greed-tm.paysys.yandex.ru%2Faliases.xml%3Ftcl_id%3D${client_id}`
            );
        });
    });
});
