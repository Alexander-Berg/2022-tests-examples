const nameFormatter = require('./nameFormatter');

it('должен капитализировать первую букву слова', () => {
    expect(nameFormatter('первое второе третье')).toEqual('Первое Второе Третье');
});

it('должен убирать множественные пробелы', () => {
    expect(nameFormatter('Первое  Второе  Третье')).toEqual('Первое Второе Третье');
});

it('должен оставлять только кириллицу и заменять латиницу', () => {
    expect(nameFormatter('Бо4нд Bdfy Бон23д')).toEqual('Бонд Иван Бонд');
});

it('не должен убирать дефис', () => {
    expect(nameFormatter('Анна-Мария Петрова')).toEqual('Анна-Мария Петрова');
});

it('должен капитализировать части двойных составляющих ФИО', () => {
    expect(nameFormatter('алексеева-сергеева анна-мария петрова')).toEqual('Алексеева-Сергеева Анна-Мария Петрова');
});
