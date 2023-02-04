const got = require('got');

async function closePool(poolId) {
    await got.post(`https://sandbox.toloka.yandex.ru/api/v1/pools/${poolId}/close`, {
        headers: {
            Authorization: `OAuth ${process.env.TOLOKA_TOKEN}`
        },
        json: true
    }).then(({body}) => body);

    await got.post(`https://sandbox.toloka.yandex.ru/api/v1/pools/${poolId}/archive`, {
        headers: {
            Authorization: `OAuth ${process.env.TOLOKA_TOKEN}`
        },
        json: true
    }).then(({body}) => body);
}

module.exports = closePool;
