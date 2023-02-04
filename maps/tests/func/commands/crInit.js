const userData = require('../credentials.js');

/**
 * Команда открывает страницу конструктора
 * если передан user, то происходит быстрый залогин
 *
 * @name browser.crInit
 * @param {String} [user] пользователь из userData
 * @param {String} [query]
 * @param {String} [state] открыть список карт (openmap) или новую карту (newmap)
 * @returns {Browser}
 */
module.exports = function (user, query = '', state) {
    const path = '/map-constructor' + query;
    const usD = user && userData[user] || null;

    return this
        .deleteCookie()
        .then(() => {
            if (usD) {
                state = state || userData.mapState.new;
                return this
                    .crLoginFast(path, usD)
                    .localStorage('POST', {key: 'cntAfterLoginState', value: state})
                    .url(path)
                    .crWaitForVisible(PO.page(), 'Конструктор не открылся')
                    .crWaitForVisible(state === userData.mapState.list ? PO.stepMapselection() : PO.stepEditor(),
                        'Не открылся шаг ' + state);
            } else {
                return this
                    .url(path)
                    .crWaitForVisible(PO.promoInitied(), 'Не открылось промо окно');
            }
        });
};
