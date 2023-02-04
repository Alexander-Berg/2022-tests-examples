const { hideElements, ignoreElements } = require('./elements');
const { Roles } = require('../../../../helpers/role_perm');

describe('user', () => {
    describe('paystep', () => {
        describe('old', () => {
            it('Выставление счета на физ.лицо с разовой клиентской скидкой', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});
                const [
                    ,
                    ,
                    request_id,
                    invoice_id
                ] = await browser.ybRun('test_request_onetime_client_discount', { login });
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.click('input[id="is_ur_0"]');
                await browser.click('input[id="paysys_id_1001"]');
                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview разовая клиентская скидка',
                    '.yb-user-content',
                    {
                        hideElements
                    }
                );

                await browser.click('input[id="gensub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'success разовая клиентская скидка',
                    '.yb-user-content',
                    {
                        hideElements: [hideElements, '.b-page-title']
                    }
                );
            });
            it('Выставление счета с агентской фиксированной скидкой', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [, person_id, contract_id, request_id, invoice_id] = await browser.ybRun(
                    'test_request_fix_agency_discount'
                );
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.click('input[id="is_ur_1"]');

                await browser.click('input[id="paysys_id_1047"]');
                await browser.click(`input[id='person_id_p${person_id}_${contract_id}c']`);
                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview агентская фиксированная скидка',
                    '.yb-user-content',
                    {
                        hideElements
                    }
                );

                await browser.click('input[id="gensub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'success агентская фиксированная скидка',
                    '.yb-user-content',
                    {
                        hideElements: [hideElements, '.b-page-title']
                    }
                );
            });
        });
    });
});
