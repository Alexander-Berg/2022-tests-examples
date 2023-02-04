'use strict';

const express = require('express');
const supertest = require('supertest');
const mw = require('../middlewares/html');
const path = require('path');

describe('middlewares/html', () => {

    it('Should send html with proper lang', (done) => {
        const app = express();

        app.use((req, res, next) => {
            res.locals.userData = {
                lang_ui: 'lang',
                foo: 'bar'
            };

            next();
        });
        app.use(mw.create({
            tmplPath: path.join(__dirname, 'mock-data/template.pug'),
            pathPrefix: 'path-prefix',
            antiCache: '123'
        }));

        supertest(app)
            .get('/')
            .expect([
                '<div>hahaha{&quot;lang_ui&quot;:&quot;lang&quot;,&quot;foo&quot;:&quot;bar&quot;}ohohoh</div>',
                '<div>pathPrefix</div>',
                '<div>antiCache</div>'
            ].join(''))
            .end(done);
    });

});
