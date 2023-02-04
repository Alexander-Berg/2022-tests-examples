/**
 * Клик на кнопку списка карт и открытие первой карты из списка
 *
 * @name browser.crOpenMap
 * @param {String} name – Название первой карты для проверки
 */
module.exports = function (name) {
    return this
        .click(PO.mapListButton())
        .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
        .crCheckText(PO.mapSelection.itemFirstName(), name, 'Название карты в списке')
        .click(PO.mapSelection.itemFirst())
        .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
        .crWaitForHidden(PO.modalCell());
};
