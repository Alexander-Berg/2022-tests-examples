const getParamsForSearchRecommendNewInStock = require('./getParamsForSearchRecommendNewInStock');

it('должен вернуть group_by=TECHPARAM,COMPLECTATION если нет catalog_filter', () => {
    const params = {};

    expect(getParamsForSearchRecommendNewInStock({ params })).toEqual({ group_by: [ 'TECHPARAM', 'COMPLECTATION' ] });
});

it('должен вернуть group_by=TECHPARAM,COMPLECTATION если catalog_filter пустой', () => {
    const params = { catalog_filter: [] };

    expect(getParamsForSearchRecommendNewInStock({ params })).toMatchObject({ group_by: [ 'TECHPARAM', 'COMPLECTATION' ] });
});

it('должен вернуть group_by=TECHPARAM,COMPLECTATION если больше одного элемента в catalog_filter', () => {
    const params = { catalog_filter: [ {}, {} ] };

    expect(getParamsForSearchRecommendNewInStock({ params })).toMatchObject({ group_by: [ 'TECHPARAM', 'COMPLECTATION' ] });
});

it('должен вернуть group_by=TECHPARAM,COMPLECTATION если в catalog_filter один элемент, но нет модели', () => {
    const params = { catalog_filter: [ {} ] };

    expect(getParamsForSearchRecommendNewInStock({ params })).toMatchObject({ group_by: [ 'TECHPARAM', 'COMPLECTATION' ] });
});

it('не должен прокидывать group_by если в catalog_filter один элемент и есть модель', () => {
    const params = { catalog_filter: [ { mark: 'mark', model: 'model' } ] };

    expect(getParamsForSearchRecommendNewInStock({ params })).toEqual({ catalog_filter: [ { mark: 'mark', model: 'model' } ] });
});
