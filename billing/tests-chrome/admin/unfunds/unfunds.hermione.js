const elements = {
    unfunds: '.yb-client-unfunds',
    invoicesLink: '.yb-client-unfunds a'
};

describe('admin', () => {
    describe('unfunds', () => {
        it('нет беззаказья', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const { client_id } = await browser.ybRun('create_client');

            await browser.ybUrl('admin', `unfunds.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(elements.unfunds);
            await browser.ybAssertView('нет беззаказья', elements.unfunds);
        });

        it('есть беззаказье, ссылка на список счетов', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const { client_id } = await browser.ybRun('create_client_with_unfunds');

            await browser.ybUrl('admin', `unfunds.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(elements.unfunds);
            await browser.ybAssertView('есть беззаказье', elements.unfunds);

            await browser.ybAssertLink(
                elements.invoicesLink,
                `/invoices.xml?client_id=${client_id}&trouble_type=5`
            );
        });
    });
});
