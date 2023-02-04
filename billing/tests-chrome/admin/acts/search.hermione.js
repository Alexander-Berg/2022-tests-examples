describe('admin', function () {
    describe('acts', function () {
        it('Проверка ссылок', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl(
                'admin',
                'acts.xml?contract_eid=ИВ-360%2F1206&client_id=393872&pn=1&ps=10&sf=act_dt&so=0'
            );
            await browser.ybWaitForLoad({ waitFilter: true });

            await browser.ybAssertLink('a=00000021167', 'act.xml?act_id=1137464');
            await browser.ybAssertLink('a=Б-1518325-1', 'invoice.xml?invoice_id=1239699');
            await browser.ybAssertLink('a=ИВ-360/1206', 'contract.xml?contract_id=6549');
        });
    });
});
