const { basicIgnore } = require('../../../helpers');
const { Roles, Perms } = require('../../../helpers/role_perm');
const {
    saveGoodDebt,
    hideElements,
    reexportStatusSelector,
    assertPageEmpty
} = require('./helpers');

describe('admin', function () {
    describe('act', function () {
        it('отображение акта и простановку хорошего долга', async function () {
            const { browser } = this;

            const [, actId] = await browser.ybRun('test_print_form_postpay_firm_7_act');
            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', 'act.xml?act_id=' + actId);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('страница акта, отображение', '.yb-content', {
                ignoreElements: basicIgnore,
                hideElements
            });

            await browser.ybAssertLink(
                'a=Перейти к печатной форме акта',
                'invoice-publish.xml?ft=html&rt=act&object_id=' + actId
            );

            await browser.ybAssertElementExists('h2*=Заметка');
            await browser.ybAssertElementExists('.yb-page-header__row=ОТДЕЛЬНЫЙ');
            await browser.ybAssertElementExists('.yb-page-header__row=ПОДРОБНЫЙ');

            await browser.ybAssertElementExists('div=(Счет-фактура № неизвестно)');
            await browser.ybAssertElementExists('div=Не выгружен в OeBS');

            await saveGoodDebt(browser);

            await browser.ybAssertView('раздел хороший долг, сохранение', '.yb-act__saver');
        });

        it('пагинация, переход по ссылкам', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'act.xml?act_id=105879250');
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'страница акта, отображение блоков с информацией об акте и о заказах',
                '.yb-content',
                {
                    hideElements: [reexportStatusSelector]
                }
            );

            await browser.ybAssertLink('a=Netpeak (5028445)', 'tclient.xml?tcl_id=5028445');
            await browser.ybAssertLink(
                'a=ИП Пискарев дмитрий (6551560)',
                'subpersons.xml?tcl_id=5028445'
            );
            await browser.ybAssertLink('a=Б-1824919064-1', 'invoice.xml?invoice_id=98688656');

            await browser.ybTableChangePageNumber(2);
            await browser.ybAssertView(
                'заказы, переключение на 2 страницу',
                '.yb-act__order-table',
                {
                    hideElements: [reexportStatusSelector]
                }
            );
            await browser.ybTableChangePageSize(25);
            await browser.ybTableScrollToEnd();
            await browser.ybAssertView(
                'заказы, переключение на вид по 25 строк',
                '.yb-act__order-table',
                {
                    hideElements: [reexportStatusSelector]
                }
            );
        });

        it('без права ViewInvoices акт не открывается', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                baseRole: Roles.ReadOnly,
                include: [],
                exclude: [Perms.ViewInvoices]
            });
            await browser.ybUrl('admin', 'act.xml?act_id=105879250');
            await assertPageEmpty(browser);
        });
    });
});
