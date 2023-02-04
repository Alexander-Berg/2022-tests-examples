import transformTagsForValidation from './transformTagsForValidation';

describe('a', () => {
    it('заменяет тег на p и сохраняет атрибуты, если отсутствует href', () => {
        const attribs = { title: 'Привет' };
        const result = transformTagsForValidation.a('a', attribs);

        expect(result.tagName).toBe('p');
        expect(result.attribs).toEqual(attribs);
    });

    it('заменяет тег на p и сохраняет атрибуты, если href это якорь', () => {
        const attribs = { href: '#subscribe' };
        const result = transformTagsForValidation.a('a', attribs);

        expect(result.tagName).toBe('p');
        expect(result.attribs).toEqual(attribs);
    });

    it('ничего не делает, если передан корректный href без якоря в начале', () => {
        const attribs = { href: 'https://mag.auto.ru/article/123#subscribe' };
        const result = transformTagsForValidation.a('a', attribs);

        expect(result.tagName).toBe('a');
        expect(result.attribs).toEqual(attribs);
    });
});
