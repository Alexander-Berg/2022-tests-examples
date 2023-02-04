'use strict';

const expect = require('chai').expect;
const politicsv2 = require('../src/lib/politics.v2');

describe('Politics v2', () => {
    it('Should set default language if unsupported language is provided', () => {
        const result = politicsv2('cu_RU', 'RU', '1');
        expect(result.language).to.eql('en');
    });

    it('Should set disputedBorders to UN if region is AQ', () => {
        const result = politicsv2('ru_AQ', 'RU', '1');
        expect(result.disputedBorders).to.eql('UN');
    });

    it('Should set disputedBorders to UN if given disputedBorders is not supported and region is 001', () => {
        const result = politicsv2('ru_001', 'EQ', '1');
        expect(result.disputedBorders).to.eql('UN');
    });

    it('Should set disputedBorders to UN if given disputedBorders is not available for a given country', () => {
        const result = politicsv2('ru_KZ', 'UA', '1');
        expect(result.disputedBorders).to.eql('UN');
    });

    it('Should set disputedBorders to RU if given disputedBorders is not supported and region is country', () => {
        const result = politicsv2('ru_UA', 'EQ', '1');
        expect(result.disputedBorders).to.eql('RU');
    });
});
