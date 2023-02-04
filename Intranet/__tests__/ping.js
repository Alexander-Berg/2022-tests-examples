'use strict';

const express = require('express');
const supertest = require('supertest');
const mw = require('../middlewares/ping');

describe('middlewares/ping', () => {

    it('Should send ok', (done) => {
        const app = express();

        app.use(mw.create());

        supertest(app)
            .get('/')
            .expect(200)
            .expect('OK')
            .end(done);
    });

});
