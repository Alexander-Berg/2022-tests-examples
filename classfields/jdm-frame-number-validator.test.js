const validator = require('./jdm-frame-number-validator');

it('должен не пропускать слишком короткую комбинацию', () => {
    expect(validator('ASDASDA')).toBe(false);
});

it('должен не пропускать слишком длинную комбинацию', () => {
    expect(validator('ASD158543367ASDAS')).toBe(false);
});

it('должен не пропускать VIN c некорректными символами (%&#,.)', () => {
    expect(validator('%&#,.1323')).toBe(false);
    expect(validator('%BC12-131323')).toBe(false);
    expect(validator('BC14.1243323')).toBe(false);
    expect(validator('F&Q1-1342123')).toBe(false);
    expect(validator(',1323BDD14000')).toBe(false);
});

it('должен пропускать корректный номер кузова', () => {
    expect(validator('GX105-6010796')).toBe(true);
    expect(validator('JZS1410055903')).toBe(true);
    expect(validator('GB3-1624892')).toBe(true);
});
