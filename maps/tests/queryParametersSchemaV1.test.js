'use strict';

const expect = require('chai').expect;
const Joi = require('joi');
const schema = require('../src/lib/queryParametersSchemas').v1;
const meta = require('../src/lib/meta');

const validLang = 'ru_RU';
const validQuality = '0';
const validDisputedBorders = 'RU';

const invalidLangWrongRegion = 'ru_AQ';

describe('Query parameters v1 schema', () => {
    describe('lang', () => {
        it('Should not return error if region is supported', () => {
            for (const region of meta.regionsv1) {
                const {error} = Joi.validate({
                    disputedBorders: validDisputedBorders,
                    lang: 'ru_' + region,
                    quality: validQuality
                }, schema);
                expect(error).to.eql(null);
            }
        });

        it('Should return error if region is unssuported', () => {
            const {error} = Joi.validate({
                disputedBorders: validDisputedBorders,
                lang: invalidLangWrongRegion,
                quality: validQuality
            }, schema);
            expect(error).to.not.eql(null);
        });
    });

    describe('All', () => {
        it('Should not return error if every argument is OK', () => {
            const {error} = Joi.validate({
                disputedBorders: validDisputedBorders,
                lang: validLang,
                quality: validQuality
            }, schema);
            expect(error).to.eql(null);
        });
    });
});
