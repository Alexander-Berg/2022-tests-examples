const filePath = './func-test/tools/session-number.txt',
    fs = require('fs');
let sessionNumber;

module.exports = () => {
    if(typeof sessionNumber === 'undefined') {
        sessionNumber = fs.readFileSync(filePath, 'utf8');
    }
    return sessionNumber;
};
