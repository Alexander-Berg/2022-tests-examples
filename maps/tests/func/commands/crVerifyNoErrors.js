/*eslint prefer-arrow-callback: "off", no-console: "off"*/

const decline = require('../utils/decline');
const Report = require('../utils/report');

const ERROR_LEVEL = {
    ERROR: 'error',
    WARNING: 'warning'
};

// TODO escape and assert errors
/**
 *
 * @name browser.crVerifyNoErrors
 */
module.exports = function () {
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

    return this
        .log('browser')
        .then((log) => {
            const val = log.value;
            let errors;
            let warnings;

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
                    console.info(createReport('WARNINGS', warnings, ERROR_LEVEL.WARNING));
                    this.setMeta('warnings', warnings.join('\n\n'));
                }
                if (errors.length) {
                    console.info(createReport('ERRORS', errors, ERROR_LEVEL.ERROR));
                    this.setMeta('errors', errors.join('\n\n'));
                }
            }

            return true;
        });
};
