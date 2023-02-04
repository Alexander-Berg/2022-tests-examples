/* eslint max-nested-callbacks: "off" */

'use strict';

const express = require('express');
const supertest = require('supertest');
const mw = require('../middlewares/user-data');
const assert = require('assert');
const cookieParser = require('cookie-parser');
const fakeBack = require('tools-access-express/tools/fake-back');

describe('middlewares/user-data', () => {

    it('Should provide res.locals.userData', (done) => {
        fakeBack((back) => {
            back.get('/api/frontend/userdata/', (req, res) => {
                res.json({user: 'vasya'});
            });
            back.start();

            const app = express();

            app.use(cookieParser());
            app.use(mw.create({
                protocol: 'http',
                port: back.port,
                hostname: back.hostname
            }));

            app.use((req, res) => {
                assert.deepEqual(res.locals.userData, {user: 'vasya'});

                res.sendStatus(200);
            });

            supertest(app)
                .get('/')
                .end(function () {
                    back.close();
                    done(...arguments);
                });
        });
    });

    it('Should redirect to passport in case of bad auth', (done) => {
        fakeBack((back) => {
            back.get('/api/frontend/userdata/', (req, res) => {
                res.sendStatus(401);
            });
            back.start();

            const app = express();

            app.use(cookieParser());
            app.use(mw.create({
                protocol: 'http',
                port: back.port,
                hostname: back.hostname
            }));

            app.use((req, res) => {
                assert.deepEqual(res.locals.userData, {user: 'vasya'});

                res.sendStatus(200);
            });

            supertest(app)
                .get('/')
                .expect('Location', 'https://passport.yandex-team.ru/auth?retpath=https%3A%2F%2F127.0.0.1%2F')
                .end(function () {
                    back.close();
                    done(...arguments);
                });
        });
    });

    it('Should handle an error from endpoint', (done) => {
        fakeBack((back) => {
            back.get('/api/frontend/userdata/', (req, res) => {
                res
                    .status(500)
                    .json({error: 'text'});
            });
            back.start();

            const app = express();

            app.use(cookieParser());
            app.use(mw.create({
                protocol: 'http',
                port: back.port,
                hostname: back.hostname
            }));

            app.use((error, req, res, next) => { // eslint-disable-line no-unused-vars
                assert.deepEqual(error.data, {error: 'text'});

                res.sendStatus(200);
            });

            supertest(app)
                .get('/')
                .end(function () {
                    back.close();
                    done(...arguments);
                });
        });

    });

});
