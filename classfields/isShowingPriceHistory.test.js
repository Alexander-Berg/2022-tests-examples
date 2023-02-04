const isShowingPriceHistory = require('./isShowingPriceHistory');
// https://st.yandex-team.ru/AUTORUFRONT-11319#5b09498080a8ff001ba93899
it('должен вернуть true, если показываем повышение цены и объявление добавлено в избранное', () => {
    expect(isShowingPriceHistory({
        is_favorite: true,
        tags: [ 'history_increase' ],
    })).toEqual(true);
});

it('должен вернуть false, если показываем повышение цены и объявление не добавлено в избранное', () => {
    expect(isShowingPriceHistory({
        is_favorite: false,
        tags: [ 'history_increase' ],
    })).toEqual(false);
});

it('должен вернуть true, если показываем понижение цены и объявление не добавлено в избранное', () => {
    expect(isShowingPriceHistory({
        is_favorite: false,
        tags: [ 'history_decrease' ],
    })).toEqual(true);
});

it('должен вернуть true, если показываем понижение цены и объявление добавлено в избранное', () => {
    expect(isShowingPriceHistory({
        is_favorite: true,
        tags: [ 'history_decrease' ],
    })).toEqual(true);
});
