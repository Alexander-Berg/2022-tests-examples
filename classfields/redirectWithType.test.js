const de = require('descript');

const RedirectError = require('auto-core/lib/handledErrors/RedirectError');
const redirectWithType = require('./redirectWithType');

let cancel;
beforeEach(() => {
    cancel = new de.Cancel();
});

it('должен вернуть CancelToken, если опции правильные', () => {
    const result = redirectWithType(cancel, {
        code: RedirectError.CODES.DESKTOP_TO_MOBILE,
        location: '/',
        status: 301,
    });

    expect(result._reason).toMatchObject({
        error: {
            code: 'DESKTOP_TO_MOBILE',
            id: 'REDIRECTED',
            location: '/',
            status_code: 301,
        },
    });
});

it('должен бросить ошибку, если нет кода', () => {
    const fn = () => {
        redirectWithType(cancel, {
            location: '/',
            status: 301,
        });
    };

    expect(fn).toThrow('Unknown RedirectError undefined');
});

it('должен бросить ошибку, если код неизвестен', () => {
    const fn = () => {
        redirectWithType(cancel, {
            code: 'no_such_code',
            location: '/',
            status: 301,
        });
    };

    expect(fn).toThrow('Unknown RedirectError no_such_code');
});

it('должен бросить ошибку, если нет location', () => {
    const fn = () => {
        redirectWithType(cancel, {
            code: RedirectError.CODES.DESKTOP_TO_MOBILE,
            status: 301,
        });
    };

    expect(fn).toThrow('Unknown redirect location undefined');
});
