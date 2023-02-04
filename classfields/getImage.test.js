const getImage = require('./getImage');

it('должен вернуть путь к изображению с известным ID', () => {
    expect(getImage('finish')).toBe('https://auto-export.s3.yandex.net/autoguru/images/finish.svg');
});

it('должен вернуть путь к изображению по умолчанию, если ID не передан', () => {
    expect(getImage('')).toBe('https://auto-export.s3.yandex.net/autoguru/images/question.svg');
});

it('должен вернуть путь к изображению по умолчанию, если ID не найден', () => {
    expect(getImage('incorrect_image_id')).toBe('https://auto-export.s3.yandex.net/autoguru/images/question.svg');
});
