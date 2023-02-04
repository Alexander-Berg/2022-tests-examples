const markModelNameplateToBcLookup = require('./markModelNameplateToBcLookup');

const TEST = [
    { mark_model_nameplate: '', bc_lookup: '' },
    { mark_model_nameplate: 'AUDI', bc_lookup: 'AUDI' },
    { mark_model_nameplate: 'VENDOR1', bc_lookup: '' },
    { mark_model_nameplate: 'AUDI#A4', bc_lookup: 'AUDI#A4' },
    { mark_model_nameplate: 'AUDI#A4##123', bc_lookup: 'AUDI#A4#123' },
    { mark_model_nameplate: 'AUDI#A4#456#123', bc_lookup: 'AUDI#A4#123' },
];

TEST.forEach(testCase => {
    it(`should convert "${ testCase.mark_model_nameplate }" to "${ testCase.bc_lookup }"`, () => {
        expect(markModelNameplateToBcLookup(testCase.mark_model_nameplate)).toEqual(testCase.bc_lookup);
    });
});
