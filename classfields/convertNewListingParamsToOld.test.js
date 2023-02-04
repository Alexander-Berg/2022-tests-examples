const convertNewListingParamsToOld = require('./convertNewListingParamsToOld');

const TESTS = require('./convertSearcherParams.testcases');

TESTS.forEach(testCase => {
    if (!testCase.public_api) {
        return;
    }

    // eslint-disable-next-line max-len
    it(`должен сконвертировать ${ JSON.stringify(testCase.public_api) } в ${ JSON.stringify(testCase.searcher) }${ testCase.category ? ' для ' + testCase.category : '' }`, () => {
        expect(convertNewListingParamsToOld(testCase.public_api, testCase.category)).toEqual(testCase.searcher);
    });
});

it('должен вернуть null, если конвертер in_stock ничего не поменял', () => {
    expect(convertNewListingParamsToOld({ in_stock: 'false' })).toBeNull();
});

it('должен вернуть null, если конвертер gear_type ничего не поменял', () => {
    expect(convertNewListingParamsToOld({
        gear_type: 'FORWARD_CONTROL',
    }, 'CARS')).toBeNull();
});

it('при конвертации из новых в старые нужно оставить старый цвет как есть', () => {
    expect(convertNewListingParamsToOld({ color: [ 'FFCC00' ] }, 'MOTO'))
        .toEqual({ moto_color: [ 'FFCC00' ] });
});

it('должен вернуть null, если конвертер cylinders ничего не поменял', () => {
    expect(convertNewListingParamsToOld({ cylinders: '2' }, 'MOTO'))
        .toBeNull();
});

it('должен вернуть null, если конвертер saddle_height ничего не поменял', () => {
    expect(convertNewListingParamsToOld({ saddle_height: '150' }, 'MOTO'))
        .toBeNull();
});

it('должен вернуть null, если конвертер strokes ничего не поменял', () => {
    expect(convertNewListingParamsToOld({ strokes: '3' }, 'MOTO'))
        .toBeNull();
});

it('должен вернуть null, если конвертер wheel_drive ничего не поменял', () => {
    expect(convertNewListingParamsToOld({ wheel_drive: '4x2' }, 'MOTO'))
        .toBeNull();
});
