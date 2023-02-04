const dir = './reports/';
const filePath = dir + 'test-time';
const filePath1 = dir + 'test-time-1.txt';
const filePath2 = dir + 'test-time-2.txt';
const fs = require('fs');

if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir);
}

fs.writeFileSync(filePath1, '');
fs.writeFileSync(filePath2, '');

module.exports = (result, fileindex = 1) => {
    fs.appendFileSync(`${filePath}-${fileindex}.txt`, result + ' ');

    return result;
};
