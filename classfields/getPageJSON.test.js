const getPageJSON = require('./getPageJSON');

beforeEach(() => {
    fetch.resetMocks();

    fetch.mockResponseOnce(JSON.stringify({}));
});

it('должен сделать запрос и правильно склеить path без params', () => {
    getPageJSON({}, '/-/ajax/path/');

    expect(fetch).toHaveBeenCalledWith(
        '/-/ajax/path/?only-data=true',
        {
            credentials: 'same-origin',
            headers: { accept: 'application/json', 'x-csrf-token': undefined, 'x-requested-with': 'XMLHttpRequest' },
            method: 'GET',
        },
    );
});

it('должен сделать запрос и правильно склеить path и params', () => {
    getPageJSON({ foo: 'bar' }, '/-/ajax/path/');

    expect(fetch).toHaveBeenCalledWith(
        '/-/ajax/path/?foo=bar&only-data=true',
        {
            credentials: 'same-origin',
            headers: { accept: 'application/json', 'x-csrf-token': undefined, 'x-requested-with': 'XMLHttpRequest' },
            method: 'GET',
        },
    );
});

it('должен сделать запрос и правильно склеить path с параметрами и params', () => {
    getPageJSON({ foo: 'bar' }, '/-/ajax/path/?baz=1');

    expect(fetch).toHaveBeenCalledWith(
        '/-/ajax/path/?baz=1&foo=bar&only-data=true',
        {
            credentials: 'same-origin',
            headers: { accept: 'application/json', 'x-csrf-token': undefined, 'x-requested-with': 'XMLHttpRequest' },
            method: 'GET',
        },
    );
});
