const saleinfo = require('./saleinfo');

describe('.showPriceHistory', function() {

    it('должен вернуть true, если есть "search_tag" с "history_discount"', () => {
        expect(saleinfo.showPriceHistory({
            search_tags: [ 'history_discount' ],
        })).toEqual(true);
    });

    it('должен вернуть false, если нет "search_tag"', () => {
        expect(saleinfo.showPriceHistory({})).toEqual(false);
    });

    it('должен вернуть false, если нет "search_tag" с "history_discount"', () => {
        expect(saleinfo.showPriceHistory({
            search_tags: [ 'discount' ],
        })).toEqual(false);
    });

});
