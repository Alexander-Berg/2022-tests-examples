/**
 *
 * @name browser.crSaveMap
 */
module.exports = function () {
    return this
        .crWaitForVisible(PO.saveAndContinue(), 'Не отображается кнопка Сохранить')
        .click(PO.saveAndContinue())
        .crWaitForVisible(PO.pagePreview(), 'Не сохранилась карта – не открылся шаг превью')
        .crWaitForHidden(PO.modalCell(), 'Паранджа не скрылась');
};
