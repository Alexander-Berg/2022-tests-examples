const castRidForSearcher = require('auto-core/lib/resource-helpers/castRidForSearcher');

it('should do nothing if no rid in params', () => {
    const params = {};
    castRidForSearcher(params);

    expect(params).toEqual({});
});

it('should do nothing if no Adygea or Krasnodar in params', () => {
    const params = { rid: 213 };
    castRidForSearcher(params);
    expect(params).toEqual({ rid: 213 });
});

it('should add Adygea if Krasnodar krai in params (single)', () => {
    const params = { rid: 10995 };
    castRidForSearcher(params);
    expect(params).toEqual({ rid: [ 10995, 11004 ] });
});

it('should add Adygea if Krasnodar krai in params (array)', () => {
    const params = { rid: 10995 };
    castRidForSearcher(params);
    expect(params).toEqual({ rid: [ 10995, 11004 ] });
});

it('should add Krasnodar krai if Adygea in params (single)', () => {
    const params = { rid: 11004 };
    castRidForSearcher(params);
    expect(params).toEqual({ rid: [ 11004, 10995 ] });
});

it('should add Krasnodar krai if Adygea in params  (array)', () => {
    const params = { rid: 11004 };
    castRidForSearcher(params);
    expect(params).toEqual({ rid: [ 11004, 10995 ] });
});
