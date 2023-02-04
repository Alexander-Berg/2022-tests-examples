const replaceLatinToCyrillic = require('./replaceLatinToCyrillic');

it('должен заменять латинские буквы на кириллицу', () => {
    expect(replaceLatinToCyrillic('bdfy bdfyjd')).toEqual('иван иванов');
});

it('не должен заменять кириллицу', () => {
    expect(replaceLatinToCyrillic('Джеймс Бонд')).toEqual('Джеймс Бонд');
});
