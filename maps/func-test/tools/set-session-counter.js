const got = require('got'),
    filePath = './func-test/tools/session-number.txt',
    fs = require('fs'),
    INTERVAL = 3000,
    MAX_ATTEMPTS = 3;

process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

function setSession() {
    return pollUntilSatisfies(
        () => got({
            url: 'https://podrick.c.maps.yandex-team.ru/nmaps-test-counter/',
            timeout: 30000,
            family: 6
        }).then(({ body }) => {
            const sessionNumber = JSON.parse(body);

            if(typeof sessionNumber === 'number') {
                fs.writeFileSync(filePath, String(sessionNumber));
            }
            else {
                throw Error('Session isn\'t number!');
            }

            return true;
        }).catch(err => {
            console.log(err);
            return false;
        }),
        status => status,
        INTERVAL,
        MAX_ATTEMPTS
    );
}

function pollUntilSatisfies(attempt, test, interval, maxAttempts) {
    return new Promise((resolve, reject) => {
        attempt().then(
            res => {
                if(test(res) || maxAttempts === 1) {
                    return resolve(res);
                }

                setTimeout(
                    () => {
                        pollUntilSatisfies(attempt, test, interval, !!maxAttempts && maxAttempts - 1).then(resolve, reject);
                    },
                    interval
                );
            },
            reject
        );
    });
}

setSession();
