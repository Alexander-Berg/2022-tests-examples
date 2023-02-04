const sort = require('./lib/sort');

it('не должен ничего делать, если нет сортировки', () => {
    const result = {};
    sort(result, undefined, 'fresh_relevance_1-desc');
    expect(result).not.toHaveProperty('sort');
});

it('должен склеить сортировку из ответа sorting', () => {
    const result = {};
    sort(result, {
        name: 'cr_date',
        desc: true,
    }, 'fresh_relevance_1-desc');
    expect(result.sort).toEqual('cr_date-desc');
});

it('должен склеить сортировку из ответа sorting, но убрать дефолт', () => {
    const result = {};
    sort(result, {
        name: 'fresh_relevance_1',
        desc: true,
    }, 'fresh_relevance_1-desc');
    expect(result).not.toHaveProperty('sort');
});
