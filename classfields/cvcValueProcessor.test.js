const cvcValueProcessor = require('./cvcValueProcessor');

describe('cvcValueProcessor', () => {
    it('вырезает не числовые символы', () => {
        expect(cvcValueProcessor('sdasdasda@*&!x<>{}')).toEqual('');
        expect(cvcValueProcessor('uh89zxcnm23z,@!z({}')).toEqual('8923');
    });

    it('позволяет вбить cvs код с ведущими нулями', () => {
        expect(cvcValueProcessor('000')).toEqual('000');
        expect(cvcValueProcessor('001')).toEqual('001');
        expect(cvcValueProcessor('011')).toEqual('011');
    });
});
