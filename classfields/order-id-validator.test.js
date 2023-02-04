const validator = require('./order-id-validator');

it('должен распознать uuid', () => {
    expect(validator('8486a639-0a21-49d3-a906-3f877b8a1e99')).toBe(true);
    expect(validator('8486a639-0a21-49d3-a906-3f877b8a1e99')).toBe(true);
});

it('должен сказать, что строка не является uuid', () => {
    expect(validator('1092321134')).toBe(false);
    expect(validator('1092321134-123-456')).toBe(false);
    expect(validator('8486a639-0a21-49d3-a96-3f877b8a1e99')).toBe(false); // на 1 символ меньше
    expect(validator('8486a639-0a21-49d3-a906-3f877b8a1e991')).toBe(false); // на 1 символ больше
    expect(validator('')).toBe(false);
    expect(validator()).toBe(false);
    expect(validator(111)).toBe(false);
});
