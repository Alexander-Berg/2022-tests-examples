import transformFromParamToVasMetrikaPlace from './transformFromParamToVasMetrikaPlace';

const testCases = [
    { input: 'card-vas', expectedResult: 'card' },
    { input: 'offers-history-reports', expectedResult: 'card' },
    { input: 'sales_list', expectedResult: 'listing' },
    { input: 'new-lk-tab', expectedResult: 'lk' },
    { input: 'desktop-lk-reseller-relative_position', expectedResult: 'lk' },
    { input: 'api_m_lk', expectedResult: 'lk' },
    { input: 'add-form', expectedResult: 'form' },
    { input: 'add_form_beta', expectedResult: 'form_beta' },
    { input: 'vas-discount-popup', expectedResult: 'other' },
];

testCases.forEach(({ expectedResult, input }) => {
    it(`возвращает корректный результат для параметра from=${ input }`, () => {
        const result = transformFromParamToVasMetrikaPlace(input);
        expect(result).toBe(expectedResult);
    });
});
