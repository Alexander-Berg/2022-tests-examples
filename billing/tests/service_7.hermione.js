describe('service_7', () => {
    describe('firm_1', () => {
        describe('RUR', () => {
            it('ur-no_contract_prepayment', async function () {
                const { browser } = this;

                await browser.ybSignIn();
                await browser.ybObjectUrl(126687711);

                await browser.ybAssertObjectView('service_7-firm_1-RUR-ur-no_contract_prepayment');
            });
        });
    });

    describe('firm_7', () => {
        describe('EUR', () => {
            it('sw_yt-no_contract_prepayment', async function () {
                const { browser } = this;

                await browser.ybSignIn();
                await browser.ybObjectUrl(126665269);

                await browser.ybAssertObjectView(
                    'service_7-firm_7-EUR-sw_yt-no_contract_prepayment',
                    { pages: 3 }
                );
            });
        });
    });
});
