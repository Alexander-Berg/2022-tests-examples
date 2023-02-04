const getEventFormattedSearchHistory = require('./getEventFormattedSearchHistory');

const event = {
    search_history: [
        { title: 'Audi TT', section: 'new' },
        { title: 'Nissan Teana', section: 'used' },
    ],
};

it('должен отдавать отформатированную историю поиска', () => {
    expect(getEventFormattedSearchHistory(event)).toEqual([ 'Новый Audi TT', 'С пробегом Nissan Teana' ]);
});
