'use strict';

const express = require('express');
const supertest = require('supertest');
const mw = require('../middlewares/redirect');

describe('middlewares/redirect', () => {

    it('Should redirect from "certificator" to "cert"', (done) => {
        const app = express();

        app.use(mw.create());

        supertest(app)
            .get('/path')
            .query({name: 'val'})
            .set('Host', 'certificator.yandex-team.ru')
            .expect(302)
            .expect('Location', 'https://crt.yandex-team.ru/path?name=val')
            .end(done);
    });

    it('Should not redirect if hostname is not certificator.yandex-team.ru', (done) => {
        const app = express();

        app.use(mw.create());
        app.use((req, res) => {
            res.sendStatus(200);
        });

        supertest(app)
            .get('/')
            .set('Host', 'foo.yandex-team.ru')
            .expect(200)
            .end(done);
    });

});
