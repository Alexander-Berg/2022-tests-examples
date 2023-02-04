'use strict';

const expect = require('chai').expect;
const politicsv1 = require('../src/lib/politics.v1');

describe('Politics v1', () => {
    it('Should set default language if unsupported language is provided', () => {
        const result = politicsv1('cu_RU', 'RU', '1');
        expect(result.language).to.eql('en');
    });

    it('Should set UN disputedBorders if region is 001 and disputedBorders are not RU or UN', () => {
        const result = politicsv1('ru_001', 'UA', '1');
        expect(result.disputedBorders).to.eql('UN');
    });

    it('Should set RU disputedBorders if region is country and disputedBorders are not supported', () => {
        const result = politicsv1('ru_RU', 'EQ', '1');
        expect(result.disputedBorders).to.eql('RU');
    });
});
