module.exports.getInvoiceDate = function () {
    const date = (x = new Date(Date.now() + 5 * 24 * 3600 * 1000));
    function pad(string) {
        return string.length === 1 ? '0' + string : string;
    }
    return (
        pad(String(x.getDate())) +
        '.' +
        pad(String(x.getMonth())) +
        '.' +
        pad(String(x.getFullYear())) +
        ' г.'
    );
};
