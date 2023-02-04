/**
 * @name browser.verifyNoErrors
 */
module.exports = async function () {
    // this.getLogs('browser')
    //     .then((log) => {
    //         const val = log.value;
    //         let warnings = [];
    //         let errors = [];

    //         if (val && val.length) {
    //             warnings = val.slice()
    //                 .filter((item) => item.level === 'WARNING')
    //                 .map((res, i) => `${i + 1}. ${res.message}`);

    //             errors = val.slice()
    //                 .filter((item) => item.level === 'SEVERE')
    //                 .map((res, i) => `${i + 1}. ${res.message}`);

    //             if (warnings.length) {
    //                 this.setMeta('warnings', warnings.join(' '));
    //             }

    //             if (errors.length) {
    //                 return this.setMeta('errors', errors.join(' ')).then(() => {
    //                     throw new Error(errorMessage(errors, 'JS error'));
    //                 });
    //             }
    //         }

    //         return true;
    //     });

    // function errorMessage(errors, message) {
    //     return (errors.length === 1 ? message : errors.length + ' ' + message + 's') +
    //         ' on the page:\n    ' + errors.join('\n    ');
    // }
};
