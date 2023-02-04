jest.mock('auto-core/react/lib/cookie');

import cookie from 'auto-core/react/lib/cookie';

import updateGeoRadiusCookie from './updateGeoRadiusCookie';

const setCookie = cookie.set as jest.MockedFunction<typeof cookie.set>;
const setCookieForever = cookie.setForever as jest.MockedFunction<typeof cookie.setForever>;

it('должен выставить куку навсегда', () => {
    updateGeoRadiusCookie(100);

    expect(setCookieForever).toHaveBeenCalledWith('gradius', '100');
});

it('должен выставить куку до конца сессии в экспе', () => {
    updateGeoRadiusCookie(100, true);

    expect(setCookie).toHaveBeenCalledWith('gradius_1000km_exp', '100');
});
