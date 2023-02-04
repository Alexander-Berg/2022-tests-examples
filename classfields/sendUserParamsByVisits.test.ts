jest.mock('auto-core/react/lib/cookie');

import cookie from 'auto-core/react/lib/cookie';

import sendUserParamsByVisits from './sendUserParamsByVisits';

const userParams = jest.fn();

const setCookie = cookie.set as jest.MockedFunction<typeof cookie.set>;
const getCookie = cookie.get as jest.MockedFunction<typeof cookie.get>;

it('Должен вызвать userParams, если визитов 5', () => {
    getCookie.mockImplementation((key?: string) => {
        if (key === 'autoru-visits-count') {
            return '5';
        }
        if (key === 'autoru-visits-session-unexpired') {
            return false;
        }

        return {};
    });
    setCookie.mockImplementation(() => {});

    expect(sendUserParamsByVisits(userParams)).toBeUndefined();
    expect(userParams).toHaveBeenCalledWith({
        visitsCount: 5,
    });
});

it('Должен вызвать userParams, если визитов 3', () => {
    getCookie.mockImplementation((key?: string) => {
        if (key === 'autoru-visits-count') {
            return '3';
        }
        if (key === 'autoru-visits-session-unexpired') {
            return false;
        }

        return {};
    });
    setCookie.mockImplementation(() => {});

    expect(sendUserParamsByVisits(userParams)).toBeUndefined();
    expect(userParams).toHaveBeenCalledWith({
        visitsCount: 3,
    });
});

it('Не должен вызвать userParams, если визитов 2', () => {
    getCookie.mockImplementation((key?: string) => {
        if (key === 'autoru-visits-count') {
            return '2';
        }
        if (key === 'autoru-visits-session-unexpired') {
            return false;
        }

        return {};
    });
    setCookie.mockImplementation(() => {});

    expect(sendUserParamsByVisits(userParams)).toBeUndefined();
    expect(userParams).not.toHaveBeenCalled();
});
