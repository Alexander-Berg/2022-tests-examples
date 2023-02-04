module.exports = function (selector, text) {
    return this
        .waitUntil(function () {
            return this.getText(selector).then(function (res) {
                return res !== text
            })
        }, 5000, 'expected ' + text + ' not to be equal')
};