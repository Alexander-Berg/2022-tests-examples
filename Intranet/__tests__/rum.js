'use strict';

const express = require('express');
const supertest = require('supertest');

const Rum = require('../middlewares/rum');

describe('middlewares/rum', () => {
    let app;

    beforeEach(() => {
        app = express();
        app.use((req, res, next) => {
            res.locals.userData = {lang_ui: 'ru'};
            res.locals.id = 'foo';

            next();
        });

        app.use(Rum.create({
            interface: 'interface',
            send: 'send',
            longtask: 'longtask',
            env: '123',
            pathPrefix: 'pathPrefix',
            antiCache: 'antiCache'
        }));

        app.use((req, res) => {
            res.json(res.locals.rum);
        });
    });

    it('Should provide RUM-counter settings', async () => {
        await supertest(app)
            .get('/')
            .then(({text}) => {
                const actual = JSON.parse(text);

                expect(actual).toMatchSnapshot();
            });
    });

    it('Should enable the debug mode if certain query param exists', async () => {
        await supertest(app)
            .get('/?rum-debug=1')
            .then(({text}) => {
                const actual = JSON.parse(text);

                expect(actual).toMatchSnapshot();
            });
    });

});

describe('middlewares/rum/rumErrorLogging', () => {
    let app;

    beforeEach(() => {
        app = express();

        app.use(Rum.ErrorLogging.create({
            interfaceOverRum: 'interface',
            implementation: 'implementation',
            filters: 'filters',
            env: '123'
        }));

        app.use((req, res) => {
            res.json(res.locals.rumErrorLogging);
        });
    });

    it('Should provide settings', async () => {
        await supertest(app)
            .get('/')
            .then(({text}) => {
                const actual = JSON.parse(text);

                expect(actual).toMatchSnapshot();
            });
    });

    it('Should enable debug mode if there is a specific query param', async () => {
        await supertest(app)
            .get('/?rum-debug=1')
            .then(({text}) => {
                const actual = JSON.parse(text);

                expect(actual).toMatchSnapshot();
            });
    });

});
