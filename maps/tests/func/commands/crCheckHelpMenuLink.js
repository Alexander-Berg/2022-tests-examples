/**
 * Команда для проверки ссылок в меню хелпа
 *
 * @name browser.crCheckHelpMenuLink
 * @param {String} selector
 * @param {String} link
 * @param {String} text
 * @param {Object} [params]
 */
module.exports = function (selector, link, text, params) {
    params = params || {};
    return this
        .click(PO.help())
        .crShouldBeVisible(PO.helpPopup())
        .crCheckWindowOpen(selector)
        .then((url) => this
            .crCheckURL(url, link, text, params)
        )
        .crWaitForHidden(PO.helpPopup(), 1000, 'Меню хелпа не закрылось после открытия ссылки');
};
