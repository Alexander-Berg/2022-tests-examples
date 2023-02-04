const statsUrlsBuilder = require('./statsUrlsBuilder');

const statsParams = {
    mark: 'MERCEDES',
    model: 'GLS_KLASSE',
};

it('Возвращает все возможные ссылки статистики цен по параметрам', () => {
    expect(statsUrlsBuilder(statsParams)).toMatchSnapshot();
});
