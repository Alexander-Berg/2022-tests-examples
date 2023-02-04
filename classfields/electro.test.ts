import seo from './electro';

it('должен правильно сформировать сео', () => {
    expect(seo()).toMatchSnapshot();
});
