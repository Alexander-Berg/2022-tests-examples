'use strict';

const expect = require('chai').expect;
const Joi = require('joi');
const schema = require('../src/lib/queryParametersSchemas').v2;
const meta = require('../src/lib/meta');

const validLang = 'ru_RU';

const validQuality = '0';
const validDisputedBorders = 'RU';

const invalidLangUnsupportedRegion = 'ru_EQ';

describe('Query parameters v2 schema', () => {
    describe('lang', () => {
        it('Should return error if region is unsupported', () => {
            const {error} = Joi.validate({
                lang: invalidLangUnsupportedRegion,
                disputedBorders: validDisputedBorders,
                quality: validQuality
            }, schema);
            expect(error).to.not.eql(null);
        });

        it('Should not return error if region is supported', () => {
            for (const region of meta.regionsv2) {
                const {error} = Joi.validate({
                    lang: 'ru_' + region,
                    disputedBorders: validDisputedBorders,
                    quality: validQuality
                }, schema);
                expect(error).to.eql(null);
            }
        });
    });

    describe('All', () => {
        it('Should not return error if every parameter is OK', () => {
            const {error} = Joi.validate({
                lang: validLang,
                disputedBorders: validDisputedBorders,
                quality: validQuality
            }, schema);
            expect(error).to.eql(null);
        });

        it('Should not return error if regions is AQ', () => {
            const {error} = Joi.validate({
                lang: validLang,
                disputedBorders: validDisputedBorders,
                quality: validQuality
            }, schema);
            expect(error).to.eql(null);
        });
    });
});
