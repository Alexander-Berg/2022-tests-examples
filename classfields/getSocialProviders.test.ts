import userMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';

import getSocialProviders from './getSocialProviders';

it('вернет провайдеров из authMethods и myUser', () => {
    const authMethods = {
        gosuslugi_oauth: true,
        mos_ru_oauth: true,
    };
    expect(getSocialProviders(userMock.data, [], authMethods)).toEqual([ 'GOSUSLUGI', 'MOSRU', 'MAILRU' ]);
});

it('поставит мос-ру в начало в москве', () => {
    const authMethods = {
        gosuslugi_oauth: true,
        mos_ru_oauth: true,
    };
    expect(getSocialProviders(userMock.data, [ 213 ], authMethods)).toEqual([ 'MOSRU', 'GOSUSLUGI', 'MAILRU' ]);
});
