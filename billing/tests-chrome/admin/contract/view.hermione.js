const { hideElements } = require('./helpers');
const { basicIgnore } = require('../../../helpers');
const { Roles, Perms } = require('../../../helpers/role_perm');

describe('admin', function () {
    describe('contract', function () {
        it('просмотр недействующего договора', async function () {
            const { browser } = this;

            const [, , contract_id, contract_eid] = await browser.ybRun('test_direct_1_contract', [
                false
            ]);
            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=${contract_id}`);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('просмотр недействующего договора', '.yb-content', {
                ignoreElements: basicIgnore,
                hideElements
            });

            await browser.ybAssertLink(
                'a=Счета по данному договору',
                `invoices.xml?contract_id=${contract_id}&contract_eid=${contract_eid}`
            );
            await browser.ybAssertLink(
                'a=Акты по данному договору',
                `acts.xml?contract_id=${contract_id}&contract_eid=${contract_eid}`
            );
            await browser.ybAssertLink(
                'a=Редактировать договор / Создать доп. соглашение',
                `contract-edit.xml?contract_id=${contract_id}&collateral_ps=1000`
            );
        });

        it('просмотр коммерческого договора [smoke]', async function () {
            const { browser } = this;

            const [, contract_id, contract_eid] = await browser.ybRun(
                'test_general_contract_ind_limits'
            );
            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=${contract_id}`);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('просмотр коммерческого договора', '.yb-content', {
                ignoreElements: basicIgnore,
                hideElements
            });

            await browser.ybAssertLink(
                'a=01',
                `contract-edit.xml?contract_id=${contract_id}&collateral_pn=1&collateral_ps=1`
            );
        });

        it('просмотр РСЯ договора', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=191478`);
            await browser.ybWaitForLoad();
            await browser.ybWaitForInvisible('.yb-export-state__spin');

            await browser.ybAssertView('просмотр РСЯ договора', '.yb-content');

            await browser.ybAssertLink(
                'a=Павлова Виктория (5392940)',
                'tclient.xml?tcl_id=5392940'
            );
            await browser.ybAssertLink(
                '.yb-contract-attributes__person',
                'subpersons.xml?tcl_id=5392940#person-2674694'
            );
        });

        it('просмотр дистрибуционного родительского договора', async function () {
            const { browser } = this;

            const [
                ,
                group_contract_id,
                first_child_contract_id,
                second_child_contract_id
            ] = await browser.ybRun('test_distribution_group_and_child_offer');
            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=${group_contract_id}`);
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'просмотр родительского дистрибуционного договора',
                '.yb-content',
                {
                    ignoreElements: basicIgnore,
                    hideElements: [
                        ...hideElements,
                        '.yb-contract-attributes-table_name_is_signed td',
                        '.yb-linked-contracts table:nth-child(2) tr:nth-child(1) td',
                        '.yb-linked-contracts table:nth-child(3) tr:nth-child(1) td'
                    ]
                }
            );

            await browser.ybAssertLink(
                'a=1',
                `contract-edit.xml?contract_id=${first_child_contract_id}`
            );
            await browser.ybAssertLink(
                'a=2',
                `contract-edit.xml?contract_id=${second_child_contract_id}`
            );
        });

        it('просмотр дистрибуционного дочернего договора', async function () {
            const { browser } = this;

            const [, group_contract_id, first_child_contract_id] = await browser.ybRun(
                'test_distribution_group_and_child_offer'
            );
            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=${first_child_contract_id}`);
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'просмотр дочернего дистрибуционного договора',
                '.yb-content',
                {
                    ignoreElements: basicIgnore,
                    hideElements: [
                        ...hideElements,
                        '.yb-contract-attributes-table_name_is_signed td',
                        '.yb-contract-attributes-table_name_parent_contract_id td',
                        '.yb-contract-attributes-table_name_distribution_tag td'
                    ]
                }
            );

            await browser.ybAssertLink(
                '.yb-contract-attributes-table_name_parent_contract_id td a',
                `contract.xml?contract_id=${group_contract_id}`
            );
        });

        it('просмотр расходного договора', async function () {
            const { browser } = this;

            const [, contract_id] = await browser.ybRun('test_spendable_dmp_contract');
            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=${contract_id}`);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('просмотр расходного договора', '.yb-content', {
                ignoreElements: basicIgnore,
                hideElements: [...hideElements]
            });
        });

        it('просмотр договора приоритетная сделка', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=199717`);
            await browser.ybWaitForLoad();
            await browser.ybWaitForInvisible('.yb-export-state__spin');

            await browser.ybAssertView('просмотр договора приоритетная сделка', '.yb-content');
        });

        it('просмотр договора справочник', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=197157`);
            await browser.ybWaitForLoad();
            await browser.ybWaitForInvisible('.yb-export-state__spin');

            await browser.ybAssertView('просмотр договора справочник', '.yb-content');
        });

        it('просмотр договора афиша', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=179817`);
            await browser.ybWaitForLoad();
            await browser.ybWaitForInvisible('.yb-export-state__spin');

            await browser.ybAssertView('просмотр договора афиша', '.yb-content');
        });

        it('просмотр договора эквайринг', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=260364`);
            await browser.ybWaitForLoad();
            await browser.ybWaitForInvisible('.yb-export-state__spin');

            await browser.ybAssertView('просмотр договора эквайринг', '.yb-content');
        });

        it('пагинация ДС', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });
            await browser.ybUrl('admin', `contract.xml?contract_id=181503`);
            await browser.ybWaitForLoad();
            await browser.ybWaitForInvisible('.yb-export-state__spin');

            await browser.ybAssertView('просмотр договора с 15 ДС', '.yb-content');

            await browser.ybTableChangePageNumber(2);
            await browser.ybWaitForInvisible('.yb-search-list__list tr:nth-child(7)');

            await browser.ybAssertView('просмотр второй страницы ДС', '.yb-contract-collaterals');
        });

        it('без права OEBS_REEXPORT_CONTRACT', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                baseRole: Roles.Support,
                include: [],
                exclude: [Perms.OEBSReexportContract]
            });
            await browser.ybUrl('admin', `contract.xml?contract_id=191478`);
            await browser.ybWaitForLoad();
            await browser.ybWaitForInvisible('.yb-export-state__spin');

            await browser.ybAssertView(
                'просмотр договора без OEBS_REEXPORT_CONTRACT',
                '.yb-content'
            );
        });
    });
});
