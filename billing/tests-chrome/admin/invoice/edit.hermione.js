describe('admin', function () {
    describe('invoice', function () {
        describe('edit', function () {
            it('изменение cпособа оплаты и плательщика', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [client_id, id] = await browser.ybRun('test_prepayment_unpaid_invoice');
                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
                await browser.ybWaitForLoad();
                const person_id = await browser.ybRun('test_create_ur_replace_person', {
                    client_id
                });
                await browser.click('a[href^="change_person.xml"]');
                await browser.click('#person_id_p' + person_id + '_c');
                await browser.click('input[value^="1033"]');
                await browser.click('#sub');
                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    'измененный способ оплаты и плательщик на paypreview.xml',
                    '.blc_header'
                );

                await browser.click('.blc_change_payment_method_or_person_button');
                await browser.ybWaitForLoad();

                await browser.ybAssertUrl(
                    '/paychoose.xml?invoice_id=' +
                        id +
                        '&paysys_id=1033' +
                        '&person_id=' +
                        person_id +
                        '&contract_id=&mode=ai'
                );

                await browser.click('#sub');
                await browser.click('.blc_submit');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'измененный плательщик в счете',
                    '.src-common-components-Person-___person-module__person-id__name'
                );
            });
        });
    });
});
