const getVinReportErrorContent = require('./getVinReportErrorContent');

it('должен вернуть текст и тайтл ошибки', () => {
    expect(getVinReportErrorContent('XYZ')).toEqual({
        text: 'Что-то пошло не так. Попробуйте еще раз.',
        title: 'Неизвестная ошибка',
    });
});
