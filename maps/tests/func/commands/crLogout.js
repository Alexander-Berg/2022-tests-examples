/**
 * Команда для выхода из аккаунта
 *
 * по-умолчанию удаляет куку логина
 * для выхода-эмуляции действия пользавателя isTrue=true
 *
 * @name browser.crLogout
 * @param {Boolean} [isTrue]
 */
module.exports = function (isTrue) {
    if (!isTrue) {
        return this
            .deleteCookie();
    }

    return this
        .click(PO.userData.pic())
        .crWaitForVisible(PO.userMenu(), 'Не открылось меню пользователя')
        .click(PO.userMenu.exit())
        .crWaitForVisible(PO.stepPromo(), 'Не открылось промо окно после выхода из аккаунта');
};
