/**
 * Команда для закрытия статуса импорта, если он остался открытым случайно
 *
 * @name browser.crCloseAdverseStatus
 *
 * */
module.exports = function () {
    return this
        .isVisible(PO.import.status()).then((val) => {
            if (val) {
                return this
                    .click(PO.import.status.abort())
                    .crWaitForHidden(PO.import.status(), 'Статус импорта не закрылся')
                    .click(PO.popupVisible.close());
            }
            return true;
        })
        .isVisible(PO.popupVisible.close()).then((val) => {
            if (val) {
                return this.click(PO.popupVisible.close());
            }
            return true;
        });
};
