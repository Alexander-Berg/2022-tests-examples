const base = require('../tslint');
module.exports = {
    'rulesDirectory': base.rulesDirectory.map((dir) => '../' + dir),
    'rules': Object.assign({}, base.rules, {
        'no-unused-expression': false,
        'no-invalid-this': false,
        'import-blacklist': false
    })
};
