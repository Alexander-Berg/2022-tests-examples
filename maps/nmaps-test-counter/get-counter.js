const createS3 = require('../../providers/s3');
const {mocks: {bucket}} = require('../../lib/config');

const s3 = createS3({
    credentials: {
        accessKeyId: process.env.MOCKS_AWS_ACCESS_KEY_ID,
        secretAccessKey: process.env.MOCKS_AWS_SECRET_ACCESS_KEY
    }
});

const NMAPS_TEST_COUNTER_KEY = 'nmaps-test-counter';
const NMAPS_TEST_COUNTER_MAX = 200;

module.exports = (_, res, next) => {
    getCounter()
        .then((counter) => setCounter((counter + 1) % NMAPS_TEST_COUNTER_MAX))
        .catch((error) => (
            error.code === 'NoSuchKey' ?
                setCounter(0) :
                Promise.reject(error)
        ))
        .then((counter) => {
            res.send(counter.toString());
        })
        .catch((error) => {
            next(error);
        });
};

function getCounter() {
    return s3
        .getObject({Bucket: bucket, Key: NMAPS_TEST_COUNTER_KEY})
        .promise()
        .then(({Body}) => Number(Body.toString()));
}

function setCounter(counter) {
    return s3
        .putObject({
            Bucket: bucket,
            Key: NMAPS_TEST_COUNTER_KEY,
            Body: counter.toString()
        })
        .promise()
        .then(() => counter);
}
