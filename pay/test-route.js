'use strict';

const express = require('express');
const router = express.Router();

const {ENV} = require('../lib/constants');
const applyOnlyIn = require('../lib/middlewares/apply-only-in');
const {html, mobile} = require('../lib/middlewares/html');
const prepareReceipts = require('../lib/middlewares/prepare-receipts');
const {pdf} = require('../lib/middlewares/pdf');

const single = require('../test/fixtures/single');
const singleMarket = require('../test/fixtures/single-market');
const singleWithContext = require('../test/fixtures/single-with-context');
const multipleWithContext = require('../test/fixtures/multiple-with-context');
const multiple = require('../test/fixtures/multiple');
const singleUber = require('../test/fixtures/uber/single');
const uberMultiple = require('../test/fixtures/uber/multiple');
const {cspReceipt} = require('../lib/middlewares/csp');
const filter = applyOnlyIn([ENV.LOCAL, ENV.TESTING, ENV.DEVELOPMENT]);

router.get('/', [
    cspReceipt,
    filter,
    (req, res, next) => {
        if (req.query.market) {
            req.body = singleMarket;
        } else if (req.query.uber) {
            req.body = singleUber;
        } else {
            req.body = single;
        }

        next();
    },
    prepareReceipts,
    html,
]);

router.get('/ctx', [
    cspReceipt,
    filter,
    (req, res, next) => {
        req.body = singleWithContext;
        next();
    },
    prepareReceipts,
    html,
]);

router.get('/pdf', [
    cspReceipt,
    filter,
    (req, res, next) => {
        if (req.query.market) {
            req.body = singleMarket;
        } else if (req.query.uber) {
            req.body = singleUber;
        } else {
            req.body = single;
        }
        next();
    },
    prepareReceipts,
    pdf,
]);

router.get('/pdf/ctx', [
    cspReceipt,
    filter,
    (req, res, next) => {
        req.body = singleWithContext;
        next();
    },
    prepareReceipts,
    pdf,
]);

router.get('/pdf/mult', [
    cspReceipt,
    filter,
    (req, res, next) => {
        req.body = req.query.uber === undefined ? multiple : uberMultiple;
        next();
    },
    prepareReceipts,
    pdf,
]);

router.get('/mobile', [
    cspReceipt,
    filter,
    (req, res, next) => {
        if (req.query.market) {
            req.body = singleMarket;
        } else if (req.query.uber) {
            req.body = singleUber;
        } else {
            req.body = single;
        }
        next();
    },
    prepareReceipts,
    mobile,
]);

router.get('/mobile/ctx', [
    cspReceipt,
    filter,
    (req, res, next) => {
        req.body = singleWithContext;
        next();
    },
    prepareReceipts,
    mobile,
]);

router.get('/mobile/mult', [
    cspReceipt,
    filter,
    (req, res, next) => {
        req.body = req.query.uber === undefined ? multiple : uberMultiple;
        next();
    },
    prepareReceipts,
    mobile,
]);

router.get('/mobile/mult/ctx', [
    cspReceipt,
    filter,
    (req, res, next) => {
        req.body = multipleWithContext;
        next();
    },
    prepareReceipts,
    mobile,
]);

module.exports = router;
