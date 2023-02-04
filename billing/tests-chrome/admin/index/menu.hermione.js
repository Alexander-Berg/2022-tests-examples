const { Roles, Perms } = require('../../../helpers/role_perm');

describe('admin', function () {
    describe('index', function () {
        it('просмотр меню', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });

            await browser.ybWaitForLoad({ waitPreloader: true });

            await browser.ybUrl('admin', 'index.xml');
            await browser.ybSetSteps('Проверяет содержимое меню');
            await browser.waitForVisible('.yb-nav');
            await browser.ybAssertView('страница, меню', '.yb-nav');

            await browser.ybAssertLink('a=Счета', 'invoices.xml');
            await browser.ybAssertLink('a=Заказы', 'orders.xml');
            await browser.ybAssertLink('a=Акты', 'acts.xml');
            await browser.ybAssertLink('a=Платежи', 'payments.xml');
            await browser.ybAssertLink(
                'a=Отчеты',
                'https://apex.balance.yandex.ru:4443/pls/metaapex/f?p=106:20',
                { isAccurate: true }
            );
            await browser.ybAssertLink('a=Клиенты', 'clients.xml');
            await browser.ybAssertLink('a=Плательщики', 'persons.xml');
            await browser.ybAssertLink('a=Кредиты', 'deferpays.xml');
            await browser.ybAssertLink('a=Договоры', 'contracts.xml');
            await browser.ybAssertLink('a=Магазин', 'products_tree.xml');
            await browser.ybAssertLink(
                'a=Спецотчеты',
                'https://apex.balance.yandex.ru:4443/pls/apex/f?p=106:16',
                { isAccurate: true }
            );
            await browser.ybAssertLink(
                'a=Оплаты',
                'https://apex.balance.yandex.ru:4443/pls/apex/f?p=106:10',
                { isAccurate: true }
            );
            await browser.ybAssertLink('a=Номенклатура', 'product-catalog.xml');
            await browser.ybAssertLink('a=ССД', 'dcs.xml');
            await browser.ybAssertLink(
                'a=Справка',
                'http://doc.yandex-team.ru/Balance/BalanceUG/concepts/About.xml',
                { isAccurate: true }
            );
        });

        it('просмотр подменю Счета', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });

            await browser.ybUrl('admin', 'index.xml');
            await browser.ybSetSteps('Проверяет содержимое подменю Счета');
            await browser.waitForVisible('.yb-nav');
            await browser.click('.yb-nav-item.yb-nav-item_item-id_45 a');
            await browser.waitForVisible('.yb-sub_visible');
            await browser.ybAssertView('страница, подменю Счета', '.yb-sub_visible');

            await browser.ybAssertLink('a=Все', 'invoices.xml');
            await browser.ybAssertLink('a=Недовыставленные счета', 'requests.xml');
        });

        it('просмотр подменю Платежи', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });

            await browser.ybUrl('admin', 'index.xml');
            await browser.ybSetSteps('Проверяет содержимое подменю Платежи');
            await browser.waitForVisible('.yb-nav');
            await browser.click('.yb-nav-item.yb-nav-item_item-id_85 a');
            await browser.waitForVisible('.yb-sub_visible');
            await browser.ybAssertView('страница, подменю Платежи', '.yb-sub_visible');

            await browser.ybAssertLink('a=Все платежи', 'payments.xml');
            await browser.ybAssertLink('a=Кредитная карта - RBS', 'payments_rbs.xml');
            await browser.ybAssertLink('a=Банковская карта - Турция', 'payments_turcard.xml');
            await browser.ybAssertLink('a=Банковская карта - Швейцария', 'payments_swcard.xml');
            await browser.ybAssertLink('a=Яндекс.Деньги', 'payments_pc.xml');
            await browser.ybAssertLink('a=WebMoney', 'payments_wm.xml');
            await browser.ybAssertLink('a=Платежи Trust', 'payments_trust.xml');
            await browser.ybAssertLink('a=Платежи PayPal', 'payments_paypal.xml');
            await browser.ybAssertLink('a=Реестры платежей', 'payments_regs.xml');
        });

        it('просмотр подменю Клиенты', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });

            await browser.ybUrl('admin', 'index.xml');
            await browser.ybSetSteps('Проверяет содержимое подменю Клиенты');
            await browser.waitForVisible('.yb-nav');
            await browser.click('.yb-nav-item.yb-nav-item_item-id_1008 a');
            await browser.waitForVisible('.yb-sub_visible');
            await browser.ybAssertView('страница, подменю Клиенты', '.yb-sub_visible');

            await browser.ybAssertLink('a=Поиск клиентов', 'clients.xml');
            await browser.ybAssertLink('a=Добавить клиента', 'addtclient.xml');
        });

        it('просмотр подменю Договоры', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });

            await browser.ybUrl('admin', 'index.xml');
            await browser.ybSetSteps('Проверяет содержимое подменю Договоры');
            await browser.waitForVisible('.yb-nav');
            await browser.click('.yb-nav-item.yb-nav-item_item-id_1101 a');
            await browser.waitForVisible('.yb-sub_visible');
            await browser.ybAssertView('страница, подменю Договоры', '.yb-sub_visible');

            await browser.ybAssertLink('a=Поиск договора', 'contracts.xml');
            await browser.ybAssertLink('a=Поиск партнерского договора', 'partner-contracts.xml');
            await browser.ybAssertLink('a=Новый договор', 'create-contract.xml');
            await browser.ybAssertLink('a=Рассылка pdf', 'pdfsend.xml');
        });

        it('просмотр подменю Поддержка', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });

            await browser.ybUrl('admin', 'index.xml');
            await browser.ybSetSteps('Проверяет содержимое подменю Поддержка');
            await browser.waitForVisible('.yb-nav');
            await browser.click('.yb-nav-item.yb-nav-item_item-id_110 a');
            await browser.waitForVisible('.yb-sub_visible');
            await browser.ybAssertView('страница, подменю Поддержка', '.yb-sub_visible');

            await browser.ybAssertLink('a=Перезабор откруток', 'completions.xml');
            await browser.ybAssertLink('a=Корректировки', 'corrections.xml');
        });
    });
});
