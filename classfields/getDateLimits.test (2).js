const getDateLimits = require('./getDateLimits');

let state;

beforeEach(() => {
    state = {
        config: {
            routeParams: {
                from: '2011-01-12',
                to: '2011-01-14',
            },
        },
        walkIn: {
            dateLimits: {
                from: '2015-05-12',
                to: '2015-05-14',
            },
        },
    };
});

it('должен селектить даты из параметров роута, пока страница грузится', () => {
    state.config.isLoading = true;

    expect(getDateLimits(state)).toEqual({
        from: '2011-01-12',
        to: '2011-01-14',
    });
});

it('должен селектить даты из домена walk-in, если страница загружена', () => {
    state.config.isLoading = false;

    expect(getDateLimits(state)).toEqual({
        from: '2015-05-12',
        to: '2015-05-14',
    });
});
