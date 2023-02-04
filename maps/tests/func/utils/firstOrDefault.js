module.exports = {
    firstOrDefault: (res) => Array.isArray(res) ? res[0] : (res || '')
};
