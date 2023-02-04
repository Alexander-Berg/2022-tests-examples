/*eslint prefer-arrow-callback: "off", no-console: "off"*/

const decline = require('../utils/decline');
const Report = require('../utils/report');

const ERROR_LEVEL = {
    ERROR: 'error',
    WARNING: 'warning'
};

/**
 *
 * @name browser.wdtVerifyNoErrors
 */
module.exports = function () {
    const browser = this.executionContext.browserId;
    if (browser !== 'chrome') {
        return true;
    }

    /**
     * Выводит в консоль ошибки из контекста браузера
     * @param {String} header
     * @param {Object[]} errors
     * @param {String} errorLevel
     */
    function createReport(header, errors, errorLevel) {
        const color = errorLevel === ERROR_LEVEL.WARNING ? 'yellow' : 'red';
        const subtitle = errorLevel === ERROR_LEVEL.WARNING ? 'предупреждение' : 'ошибка';

        const count = errors.length;
        const head = `${header} │ ${count} ${decline(subtitle, count)}`;
        const report = new Report({head, color});

        return report.add(errors).toString();
    }

    let ignoreErrors = false;

    return this
        .getMeta('ignoreErrors')
        .then((isIgnore) => {
            ignoreErrors = isIgnore;
            return true;
        })
        .log('browser')
        .then((log) => {
            const val = log.value;
            let warnings = [];
            let errors = [];

            if (val && val.length) {
                warnings = val.slice()
                    .filter(function (item) {
                        return item.level === 'WARNING';
                    })
                    .map((res, i) => `${i + 1}. ${res.message}`);

                errors = val.slice()
                    .filter(function (item) {
                        return item.level === 'SEVERE';
                    })
                    .map((res, i) => `${i + 1}. ${res.message}`);

                if (warnings.length) {
                    this.setMeta('warnings', warnings);
                    console.info(createReport('WARNINGS', warnings, ERROR_LEVEL.WARNING));
                }
                if (errors.length) {
                    this.setMeta('errors', errors);
                    console.info(createReport('ERRORS', errors, ERROR_LEVEL.ERROR));
                    return ignoreErrors ? true : assert.isNotOk('Errors!', 'Есть ошибки в консоли');
                }
            }

            return true;
        });
};
