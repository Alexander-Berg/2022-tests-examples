const { assertViewOpts, waitTimeoutForExtensiveQuery } = require('./helpers');

describe('admin', function () {
    describe('credits', function () {
        it('нет кредитов', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            const clientId = await browser.ybRun('test_empty_agency');

            await browser.ybUrl('admin', 'credits.xml?tcl_id=' + clientId);
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'кредиты, просмотр когда нет кредитов',
                '.yb-client-page__client-data .yb-page-section',
                assertViewOpts
            );
        });

        it('кредит в валюте старый лс', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            const { client_id } = await browser.ybRun('test_credits_currency');

            await browser.ybUrl('admin', 'credits.xml?tcl_id=' + client_id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'кредиты, просмотр когда кредит в валюте старый лс',
                '.yb-contract',
                assertViewOpts
            );
        });

        it('индивидуальные лимиты, новый лс, несколько договоров, ссылки', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            const { invoice_id, contract_id, client_id, individ_invoice_id } = await browser.ybRun(
                'test_credits_agency_with_individual_limits'
            );

            await browser.ybUrl('admin', 'credits.xml?tcl_id=' + client_id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'кредиты, просмотр когда индивидуальные лимиты, новый лс, несколько договоров',
                '.yb-client-page__client-data',
                { ...assertViewOpts, hideElements: ['.yb-credits-restrictions'] }
            );

            await browser.ybAssertLink(
                'a=test-hermione-contract-nonres-credits-01/2',
                `contract.xml?contract_id=${contract_id}`
            );
            await browser.ybAssertLink('a=2 021,75', `invoice.xml?invoice_id=${invoice_id}`);
            await browser.ybAssertLink('a=1,41', `invoice.xml?invoice_id=${individ_invoice_id}`);
        });

        it('неактивные договоры', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            const { client_id } = await browser.ybRun('test_credits_nonactive_contracts');

            await browser.ybUrl('admin', 'credits.xml?tcl_id=' + client_id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'кредиты, просмотр неактивных договоров',
                '.yb-client-page__client-data',
                assertViewOpts
            );
        });

        it('фиктивная схема', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            const { contract_id, client_id } = await browser.ybRun('test_credits_fictive_scheme');

            await browser.ybUrl('admin', 'credits.xml?tcl_id=' + client_id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'кредиты, просмотр фиктивной схемы',
                '.yb-contract',
                assertViewOpts
            );

            await browser.ybAssertLink(
                'a.yb-spent-value',
                `invoices.xml?contract_id=${contract_id}&contract_eid=test-hermione-contract-credits-04/1&post_pay_type=3`
            );
        });

        it('информация по блокировкам, ссылки', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'credits.xml?tcl_id=434544');
            await browser.ybWaitForLoad({ preloaderTimeout: waitTimeoutForExtensiveQuery });

            await browser.ybAssertView(
                'кредиты, просмотр информации по блокировкам',
                '.yb-credits-restrictions',
                {
                    ...assertViewOpts,
                    captureElementFromTop: true,
                    allowViewportOverflow: true,
                    compositeImage: true
                }
            );

            await browser.ybAssertLink('a=32318/15', `contract.xml?contract_id=202884`);
            await browser.ybAssertLink('a=Б-1570576207-1', `invoice.xml?invoice_id=91103936`);
        });
    });
});
