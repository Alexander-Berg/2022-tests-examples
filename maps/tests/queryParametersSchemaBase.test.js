'use strict';

const expect = require('chai').expect;
const Joi = require('joi');
const schema = require('../src/lib/queryParametersSchemas').base;

const validQuality = '0';
const validDisputedBorders = 'RU';
const validLang = 'ru_RU';

const invalidLangWrongFormat = 'ruru_RURU';
const invalidQuality = '10';

describe('Query parameters base schema', () => {
    describe('quality', () => {
        it('Should return error if quality is not provided', () => {
            const {error} = Joi.validate({
                disputedBorders: validDisputedBorders,
                quality: undefined,
                lang: validLang
            }, schema);
            expect(error).to.not.eql(null);
        });

        it('Should return error if quality value is unsuported', () => {
            const {error} = Joi.validate({
                disputedBorders: validDisputedBorders,
                quality: invalidQuality,
                lang: validLang
            }, schema);
            expect(error).to.not.eql(null);
        });
    });

    describe('disputedBorders', () => {
        it('Should return error if disputedBorders undefined', () => {
            const {error} = Joi.validate({
                disputedBorders: undefined,
                quality: validQuality,
                lang: validLang
            }, schema);
            expect(error).to.not.eql(null);
        });
    });

    describe('lang', () => {
        it('Should return error if lang param is undefined', () => {
            const {error} = Joi.validate({
                disputedBorders: validDisputedBorders,
                quality: validQuality,
                lang: undefined
            }, schema);
            expect(error).to.not.eql(null);
        });

        it('Should return error if lang param is in wrong format', () => {
            const {error} = Joi.validate({
                disputedBorders: validDisputedBorders,
                quality: validQuality,
                lang: invalidLangWrongFormat
            }, schema);
            expect(error).to.not.eql(null);
        });
    });

    describe('All', () => {
        it('Should not return error if every parameter is OK', () => {
            const {error} = Joi.validate({
                disputedBorders: validDisputedBorders,
                quality: validQuality,
                lang: validLang
            }, schema);
            expect(error).to.eql(null);
        });
    });
});
