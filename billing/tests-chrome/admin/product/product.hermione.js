const { basicIgnore } = require('../../../helpers');

const elements = {
    pricesSection: '.yb-product-prices',
    taxesSection: '.yb-product-taxes',
    markupsSection: '.yb-product-markups',
    seasonCoeffsSection: '.yb-product-season-coefficient'
};
const { Roles } = require('../../../helpers/role_perm');

/*
когда будет настроен экспорт тестовых продуктов из mdh,
можно завести в проде тестовый продукт с максимальным набором параметров
и сократить количество используемых в тесте продуктов
*/

describe('admin', () => {
    describe('product', () => {
        it('страница продукта, информация, наименования', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            await browser.ybUrl('admin', `product.xml?product_id=507862`);
            await browser.ybWaitForLoad();
            await browser.ybAssertView('страница продукта', '.yb-content', {
                ignoreElements: basicIgnore
            });

            await browser.ybUrl('admin', `product.xml?product_id=511550`);
            await browser.ybWaitForLoad();
            await browser.ybAssertView('страница продукта 2', '.yb-content', {
                ignoreElements: basicIgnore
            });
        });

        it('пагинация, кол-во элементов, налоги, цены, коэф-ты, маркапы', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            await browser.ybUrl('admin', `product.xml?product_id=504083`);
            await browser.ybWaitForLoad();

            await browser.ybTableChangePageNumber(2, elements.pricesSection);
            await browser.ybAssertView(
                'изменения цены, переключение на 2 страницу',
                elements.pricesSection
            );

            await browser.ybTableChangePageSize(25, elements.pricesSection);
            await browser.ybAssertView(
                'изменения цены, переключение на 25 элементов',
                elements.pricesSection
            );

            await browser.ybUrl('admin', `product.xml?product_id=508588`);
            await browser.ybWaitForLoad();

            await browser.ybTableChangePageNumber(2, elements.taxesSection);
            await browser.ybAssertView(
                'изменения налогов, переключение на 2 страницу',
                elements.taxesSection
            );

            await browser.ybTableChangePageSize(25, elements.taxesSection);
            await browser.ybAssertView(
                'изменения налогов, переключение на 25 элементов',
                elements.taxesSection
            );

            await browser.ybUrl('admin', `product.xml?product_id=2468`);
            await browser.ybWaitForLoad();

            await browser.ybTableChangePageNumber(2, elements.markupsSection);
            await browser.ybAssertView(
                'маркапы, переключение на 2 страницу',
                elements.markupsSection
            );

            await browser.ybTableChangePageSize(25, elements.markupsSection);
            await browser.ybAssertView(
                'маркапы, переключение на 25 элементов',
                elements.markupsSection
            );

            await browser.ybTableChangePageNumber(2, elements.seasonCoeffsSection);
            await browser.ybAssertView(
                'сезонные коэффициенты, переключение на 2 страницу',
                elements.seasonCoeffsSection
            );

            await browser.ybTableChangePageSize(25, elements.seasonCoeffsSection);
            await browser.ybAssertView(
                'сезонные коэффициенты, переключение на 25 элементов',
                elements.seasonCoeffsSection
            );
        });

        it('без прав ViewProduct', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.ClientManager, include: [], exclude: [] });

            await browser.ybUrl('admin', `product.xml?product_id=1475`);

            await browser.ybWaitForLoad();
            await browser.ybAssertView('ошибка доступа', '.yb-content', {
                ignoreElements: basicIgnore
            });
        });
    });
});
