// balance.real_builders.clients.clients.test_empty_agency - агентство без скидок
// balance.real_builders.clients.discounts.test_discounts[fixed and scale] - агентство c фиксированной скидкой и скидкой по шкале
// balance.real_builders.clients.discounts.test_discounts[fixed] - агентство c фиксированной скидкой
// balance.real_builders.clients.discounts.test_discounts[scale] - агентство cо скидкой по шкале

const elements = {
    contractLink: '.yb-page-section table tbody tr td:first-child a',
    pageSection: '.yb-client-page__client-data .yb-page-section',
    emptyPageSection: '.yb-page-section*=Нет',
    list: '.yb-client-page__client-data .yb-page-section table'
};

const options = {
    hideElements: ['.yb-client-page__client-data table tbody tr td:first-child a']
};

describe('admin', () => {
    describe('discounts', () => {
        it('агентство без скидок', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const { client_id } = await browser.ybRun('create_empty_agency');

            await browser.ybUrl('admin', `discounts.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(elements.emptyPageSection);
            await browser.ybAssertView('агентство без скидок', elements.pageSection);
        });

        it('агентство c фиксированной скидкой и скидкой по шкале', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const { client_id } = await browser.ybRun('client_discounts', [true, true]);

            await browser.ybUrl('admin', `discounts.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(elements.list);
            await browser.ybSetSteps(
                'При снятии скриншота скрываем ссылку на договор для стабильности теста'
            );
            await browser.ybAssertView(
                'агентство c фиксированной скидкой и скидкой по шкале',
                elements.pageSection,
                options
            );
        });

        it('агентство c фиксированной скидкой', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const { client_id } = await browser.ybRun('client_discounts', [true, false]);

            await browser.ybUrl('admin', `discounts.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(elements.list);
            await browser.ybSetSteps(
                'При снятии скриншота скрываем ссылку на договор для стабильности теста'
            );
            await browser.ybAssertView(
                'агентство c фиксированной скидкой',
                elements.pageSection,
                options
            );

            await browser.ybSetSteps(
                'Кликает на ссылку на договор и проверяет, что загрузилась страница договора'
            );
            await browser.click(elements.contractLink);
            await browser.waitForVisible('.yb-copyable-header__title*=Договор');
        });

        it('агентство cо скидкой по шкале', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const { client_id } = await browser.ybRun('client_discounts', [false, true]);

            await browser.ybUrl('admin', `discounts.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(elements.list);
            await browser.ybSetSteps(
                'При снятии скриншота скрываем ссылку на договор для стабильности теста'
            );
            await browser.ybAssertView(
                'агентство cо скидкой по шкале',
                elements.pageSection,
                options
            );
        });
    });
});
