module.exports = function (afterEach) {
    afterEach(function () {
        return this.browser
            .crVerifyNoErrors();
    });
};
