/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import { SocialProvider } from '@vertis/schema-registry/ts-types-snake/vertis/common';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import userMock from 'auto-core/react/dataDomain/user/mocks';

import GosUslugiBenefit from './GosUslugiBenefit';
import type { OwnProps } from './GosUslugiBenefit';

let props: OwnProps;

beforeEach(() => {
    props = {
        user: userMock.withAuth(true).value()['data'],
        onAccountConnect: () => {},
    };
});

describe('метрика', () => {
    it('при показе блока пользователю без привязанного аккаунта', () => {
        shallowRenderComponent({ props });

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show-request', 'add_gosuslugi' ]);
    });

    it('при показе блока пользователю с привязанным аккаунтом', () => {
        props.user = userMock.withAuth(true).withSocialProfiles([ {
            provider: SocialProvider.GOSUSLUGI,
            social_user_id: '1',
            nickname: 'jd',
            first_name: 'john',
            last_name: 'doe',
            trusted: true,
        } ]).value().data;

        shallowRenderComponent({ props });

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show-request', 'verify_gosuslugi' ]);
    });

    it('при показе попапа пользователю без привязанного аккаунта', () => {
        const page = shallowRenderComponent({ props });

        contextMock.metrika.sendPageEvent.mockClear();

        const popup = page.find('.GosUslugiBenefit__popupAnchor');
        popup.simulate('showPopup');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show-request-popup', 'add_gosuslugi' ]);
    });

    it('при показе попапа пользователю с привязанным аккаунтом', () => {
        props.user = userMock.withAuth(true).withSocialProfiles([ {
            provider: SocialProvider.GOSUSLUGI,
            social_user_id: '1',
            nickname: 'jd',
            first_name: 'john',
            last_name: 'doe',
            trusted: true,
        } ]).value().data;

        const page = shallowRenderComponent({ props });

        contextMock.metrika.sendPageEvent.mockClear();

        const popup = page.find('.GosUslugiBenefit__popupAnchor');
        popup.simulate('showPopup');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show-request-popup', 'verify_gosuslugi' ]);
    });
});

function shallowRenderComponent({ props }: { props: OwnProps }) {
    const ContextProvider = createContextProvider(contextMock);

    const page = shallow(
        <ContextProvider>
            <GosUslugiBenefit { ...props }/>
        </ContextProvider>,
    );

    return page.dive().dive();
}
