const staticPagesUrlBuilder = require('./staticPagesUrlBuilder');

it('Возвращает список статичных страниц для сайтмапа', () => {
    expect(staticPagesUrlBuilder()).toMatchSnapshot();
});
