/*global Buffer*/

/**
 * Открывает страницу для тестирования и логинит пользователя
 *
 * @param {String} path – строка запроса с параметрами
 * @param {Object} user – пользователь из userData
 * @returns {Browser}
 */
module.exports = function (path, user) {
    const passportUrl = 'https://passport.yandex.ru/passport?mode=auth&retpath=' +
        encodeURIComponent(this.options.baseUrl + path) + '&from=map-constructor';
    const timestamp = Math.round(new Date().getTime() / 1000);
    const html = `
        <html>
            <form method="POST" action="${passportUrl}">
                <input name="login" value="${user.login}">
                <input name="passwd" value="${user.pass}">
                <input type="checkbox" name="twoweeks" value="no">
                <input type="hidden" name="timestamp" value="${timestamp}">
                <button type="submit">Login</button>
            </form>
        <html>
    `;

    const base64 = Buffer.from(html).toString('base64');

    return this
        .url('data:text/html;base64,' + base64)
        .waitForExist('form')
        .submitForm('form');
};
