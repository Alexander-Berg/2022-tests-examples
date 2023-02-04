describe('user', () => {
    describe('invoice-by-eid', () => {
        it('открытие счета по external_id', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('user', `invoice-by-eid.xml?invoice_eid=Б-1768-1`);

            await browser.waitForVisible('#invoice_consumes-list-container .show-order');
            await browser.waitForVisible('#invoice_acts_data div');
            await browser.ybAssertView(
                'просмотр счета, открытого по external_id',
                '.yb-user-content'
            );
        });
    });
});
