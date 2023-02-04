const React = require('react');
const renderer = require('react-test-renderer');

const SearchLineSuggestHighlight = require('./SearchLineSuggestHighlight');

it('должен правильно подкрасить весь запрос', () => {
    const tree = renderer.create(
        <SearchLineSuggestHighlight
            item={{
                tokens: [
                    { start_char: 0, end_char: 7, type: 'UNRECOGNIZED_FRAGMENT' },
                    { start_char: 22, end_char: 29, type: 'UNRECOGNIZED_FRAGMENT' },
                    { start_char: 31, end_char: 36, type: 'UNRECOGNIZED_FRAGMENT' },
                ],
            }}
            query="странный ауди седан с лазерной пушкой"
        />,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});
