const got = require('got');

function getPools() {
    return got.get('https://sandbox.toloka.yandex.ru/api/v1/pools/', {
        headers: {
            Authorization: `OAuth ${process.env.TOLOKA_TOKEN}`
        },
        json: true,
        query: {
            status: 'OPEN',
            sort: 'created'
        }
    }).then(({body}) => body);
}

module.exports = getPools;
