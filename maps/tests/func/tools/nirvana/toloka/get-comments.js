const got = require('got');

function getComments(poolId) {
    return got.get('https://sandbox.toloka.yandex.ru/api/v1/assignments/', {
        headers: {
            Authorization: `OAuth ${process.env.TOLOKA_TOKEN}`
        },
        json: true,
        query: {
            pool_id: poolId,
            limit: 1000
        }
    }).then(({body}) => body);
}

module.exports = getComments;
