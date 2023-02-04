const fs = require('fs');
const http = require('http');

const rimraf = require('rimraf');
const express = require('express');
const { json, urlencoded } = require('body-parser');

const { serverPort, testRunType } = require('./config');

const { getBaseFixturesDirectory, getFixtureName, getFixturePath } = require('./utils');

const Server = (responseTransformRules = {}) => {
    const counters = {};
    const app = express();

    app.use(json({ limit: '200mb' }));
    app.use(urlencoded({ extended: true }));

    app.post('/read', (req, res) => {
        try {
            const { key = {} } = req.body;
            const { resourceName, methodName } = key;
            const fixtureName = getFixtureName(key);
            const orderNum = counters[fixtureName] || 1;

            const filename = getFixturePath({
                key,
                orderNum,
            });

            const fixtureExists = fs.existsSync(filename);

            // Файл должен быть, но его нет
            if (testRunType === 'PLAY' && !fixtureExists) {
                throw new Error(`Файл ${filename} не найден`);
            }

            // Файл должен быть, и он есть
            if (['PLAY', 'UPDATE'].includes(testRunType) && fixtureExists) {
                const fixture = fs.readFileSync(filename, 'utf8');
                let response;
                let isJSON;

                try {
                    response = JSON.parse(fixture);
                    isJSON = true;
                } catch (e) {
                    response = fixture;
                    isJSON = false;
                }

                const transformRule = (responseTransformRules[resourceName] || {})[methodName];

                if (transformRule) {
                    let transformFn;

                    // eslint-disable-next-line banhammer/no-restricted-functions
                    if (Array.isArray(transformRule)) {
                        transformFn = transformRule.find(({ order }) => order === orderNum) || {};
                        transformFn = transformFn.fn;
                    } else {
                        transformFn = transformRule.fn;
                    }
                    if (transformFn) {
                        response = transformFn(response);
                    }
                }

                counters[fixtureName] = orderNum + 1;

                return isJSON ? res.json(response) : res.send(response);
            }

            return res.send(undefined);
        } catch (error) {
            return res.status(500).json({ error: error.message });
        }
    });

    app.post('/write', (req, res) => {
        try {
            const { key = {}, payload = {} } = req.body;

            const { resourceName, methodName } = key;
            const fixtureName = getFixtureName(key);
            const orderNum = counters[fixtureName] || 1;

            const filename = getFixturePath({
                key,
                orderNum,
            });

            const fixtureExists = fs.existsSync(filename);

            // Файл нужно создать
            if (testRunType === 'CREATE' || (testRunType === 'UPDATE' && !fixtureExists)) {
                let fixture = payload;
                const transformRule = (responseTransformRules[resourceName] || {})[methodName];

                if (transformRule) {
                    let transformFn;

                    // eslint-disable-next-line banhammer/no-restricted-functions
                    if (Array.isArray(transformRule)) {
                        transformFn = transformRule.find(({ order }) => order === orderNum) || {};
                        transformFn = transformFn.fn;
                    } else {
                        transformFn = transformRule.fn;
                    }
                    if (transformFn) {
                        fixture = transformFn(fixture);
                    }
                }

                const response = fixture;
                const isJSON = fixture && typeof fixture === 'object';

                if (isJSON) {
                    fixture = JSON.stringify(fixture, null, 2);
                }

                fs.writeFileSync(filename, fixture, 'utf8');
                counters[fixtureName] = orderNum + 1;

                return isJSON ? res.json(response) : res.send(response);
            }

            return res.send(undefined);
        } catch (error) {
            return res.status(500).json({ error: error.message });
        }
    });

    const server = http.createServer(app);

    return {
        start: () => {
            if (testRunType === 'CREATE') {
                rimraf.sync(getBaseFixturesDirectory());
            }
            server.listen(serverPort);
        },
        stop: () => {
            server.close();
        },
    };
};

module.exports = { Server };
