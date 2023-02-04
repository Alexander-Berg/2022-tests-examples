require('../helper.js')(afterEach);

describe('Промо окно', () => {
    it('Создание карты', function () {
        return this.browser
            .crInit()
            .click(PO.promo.create())
            .crLogin('ONE_MAP')
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .crShouldNotBeVisible(PO.mapSelection())
            .crLogout(true);
    });

    it('Открытие списка карт', function () {
        return this.browser
            .crInit()
            .click(PO.promo.open())
            .crLogin('ONE_MAP')
            .crWaitForVisible(PO.stepMapselection(), 'Не открылся список карт')
            .crShouldBeVisible(PO.mapSelectionActive())
            .click(PO.mapSelection.close())
            .crLogout(true);
    });

    it('Пустой список карт', function () {
        return this.browser
            .crInit()
            .click(PO.promo.open())
            .crLogin('NO_MAP')
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .crShouldNotBeEnabled(PO.mapListButton())
            .click(PO.mapListButton())
            .crShouldNotBeVisible(PO.stepMapselection())
            .crLogout(true);
    });

    it('Создать карту с пустым списком карт', function () {
        return this.browser
            .crInit()
            .click(PO.promo.create())
            .crLogin('NO_MAP')
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .crShouldNotBeEnabled(PO.mapListButton())
            .crShouldNotBeVisible(PO.promo())
            .crShouldNotBeVisible(PO.mapSelectionActive())
            .crShouldNotBeVisible(PO.mapSelection())
            .click(PO.mapListButton())
            .crShouldNotBeVisible(PO.stepMapselection())
            .crLogout(true);
    });

    it('Обновление страницы со списком карт', function () {
        return this.browser
            .crInit()
            .click(PO.promo.open())
            .crLogin('ONE_MAP')
            .crWaitForVisible(PO.stepMapselection(), 'Не открылся список карт')
            .click(PO.mapSelection.close())
            .refresh()
            .crWaitForVisible(PO.stepMapselection(), 'Не открылся список карт')
            .click(PO.mapSelection.close())
            .crLogout(true);
    });

    it('Обновление страницы с пустым списком карт', function () {
        return this.browser
            .crInit()
            .click(PO.promo.open())
            .crLogin('NO_MAP')
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .refresh()
            .crWaitForVisible(PO.stepPromo(), 'Не открылось промо окно')
            .crShouldNotExist(PO.promo.open())
            .click(PO.promo.close())
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .crLogout(true);
    });

    it('Обновление страницы после создания карты не с пустым списком карт', function () {
        return this.browser
            .crInit()
            .click(PO.promo.create())
            .crLogin('ONE_MAP')
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .click(PO.mapListButton())
            .crWaitForVisible(PO.stepMapselection(), 'Не открылся список карт')
            .crShouldBeVisible(PO.mapSelectionActive())
            .click(PO.mapSelection.close())
            .crShouldNotBeVisible(PO.mapSelection())
            .crLogout(true);
    });
});
