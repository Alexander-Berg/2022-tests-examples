import React from 'react';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';

import type { CardTypeInfo_CardType as Type } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';
import { CardTypeInfo_CardType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import PageGarageAddCardButton from './PageGarageAddCardButton';

const Context = createContextProvider(contextMock);

type RenderProps = {
    type: Type;
    isAuth?: boolean;
}

const renderComponent = ({ isAuth, type }: RenderProps) => {
    return render(
        <Context>
            <Provider store={ mockStore({ vinCheckInput: { value: 'A000AA00' } }) }>
                <PageGarageAddCardButton
                    type={ type }
                    isAuth={ isAuth }
                />
            </Provider>
        </Context>,
    );
};

it('ссылка должна вести на страницу добавления', async() => {
    const { getByRole } = await renderComponent({ type: CardTypeInfo_CardType.CURRENT_CAR, isAuth: true });
    const link = getByRole('link');
    expect(link.getAttribute('href')).toEqual('link/garage-add-card/?');
});

it('ссылка должна вести на страницу авторизации', async() => {
    const { getByRole } = await renderComponent({ type: CardTypeInfo_CardType.CURRENT_CAR, isAuth: false });
    const link = getByRole('link');
    expect(link.getAttribute('href')).toEqual('https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Fundefinedlink%2Fgarage-add-card%2F%3F');
});
