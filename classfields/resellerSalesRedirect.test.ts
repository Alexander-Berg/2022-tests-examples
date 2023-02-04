jest.mock('auto-core/server/descript/redirectWithType');

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import userMock from 'auto-core/react/dataDomain/user/mocks';

import redirectWithType from 'auto-core/server/descript/redirectWithType';
import type { TDescriptContext } from 'auto-core/server/descript/createContext';

import type { TOfferCategory } from 'auto-core/types/proto/auto/api/api_offer_model';

import resellerSalesRedirect from './resellerSalesRedirect';

const cancelMock = { cancel: jest.fn() };
const contextMock = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

const redirectWithTypeMock = redirectWithType as jest.MockedFunction<typeof redirectWithType>;
redirectWithTypeMock.mockReturnValue({ cancel: () => {} });

it('анона отправит за авторизацией', () => {
    resellerSalesRedirect({
        cancel: cancelMock,
        params: { category: 'cars' as TOfferCategory },
        context: contextMock,
        result: userMock.withAuth(false).value().data,
    });

    expect(redirectWithTypeMock).toHaveBeenCalledTimes(1);
    expect(redirectWithTypeMock).toHaveBeenCalledWith(cancelMock, {
        code: 'AUTH',
        location: 'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Fundefined',
        status: 302,
    });
});

it('дилера отправит в кабинет', () => {
    resellerSalesRedirect({
        cancel: cancelMock,
        params: { category: 'cars' as TOfferCategory },
        context: contextMock,
        result: userMock.withDealer(true).value().data,
    });

    expect(redirectWithTypeMock).toHaveBeenCalledTimes(1);
    expect(redirectWithTypeMock).toHaveBeenCalledWith(cancelMock, {
        code: 'LK_TO_CABINET',
        location: 'https://cabinet.autoru_frontend.base_domain/',
        status: 302,
    });
});

it('обычного пользователя отправит в лк', () => {
    resellerSalesRedirect({
        cancel: cancelMock,
        params: { category: 'cars' as TOfferCategory },
        context: contextMock,
        result: userMock.withAuth(true).value().data,
    });

    expect(redirectWithTypeMock).toHaveBeenCalledTimes(1);
    expect(redirectWithTypeMock).toHaveBeenCalledWith(cancelMock, {
        code: 'LK_MAIN_TO_CATEGORY',
        location: 'https://autoru_frontend.base_domain/my/cars/',
        status: 302,
    });
});

it('обычного пользователя в комтс отправит в лк в правильную категорию', () => {
    resellerSalesRedirect({
        cancel: cancelMock,
        params: { category: 'trucks' as TOfferCategory },
        context: contextMock,
        result: userMock.withAuth(true).value().data,
    });

    expect(redirectWithTypeMock).toHaveBeenCalledTimes(1);
    expect(redirectWithTypeMock).toHaveBeenCalledWith(cancelMock, {
        code: 'LK_MAIN_TO_CATEGORY',
        location: 'https://autoru_frontend.base_domain/my/trucks/',
        status: 302,
    });
});

it('премиум перекупа никуда не будет отправлять', () => {
    resellerSalesRedirect({
        cancel: cancelMock,
        params: { category: 'cars' as TOfferCategory },
        context: contextMock,
        result: userMock.withAuth(true).withPremiumReseller(true).value().data,
    });

    expect(redirectWithTypeMock).toHaveBeenCalledTimes(0);
});
