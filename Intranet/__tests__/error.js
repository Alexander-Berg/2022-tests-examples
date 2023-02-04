'use strict';

const express = require('express');
const supertest = require('supertest');
const mw = require('../middlewares/error');

describe('middlewares/error', () => {

    it('Should handle an error from the previous middlewares', (done) => {
        const app = express();

        app.use((req, res, next) => {
            const error = new Error('error text');

            error.data = {foo: 'bar'};

            next(error);
        });
        app.use(mw.except());

        supertest(app)
            .get('/')
            .expect(500)
            .expect('{"name":"Error","message":"error text","data":{"foo":"bar"}}')
            .end(done);
    });

});
