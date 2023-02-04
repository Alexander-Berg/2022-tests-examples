import React from 'react';
import { render, screen } from '@testing-library/react';

import { CardTypeInfo_CardType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';
import type { CardTypeInfo_CardType as TCardType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import GarageCardSelectorAddItem from './GarageCardSelectorAddItem';

const Context = createContextProvider(contextMock);

const renderComponent = (type?: TCardType) => {
    return render(
        <Context>
            <GarageCardSelectorAddItem type={ type }/>
        </Context>,
    );
};

describe('правильная ссылка', () => {
    it('current', () => {
        renderComponent(CardTypeInfo_CardType.CURRENT_CAR);
        const link = screen.getByRole('link');
        expect(link.getAttribute('href')).toBe('link/garage-add-card/?');
    });

    it('dream', () => {
        renderComponent(CardTypeInfo_CardType.DREAM_CAR);
        const link = screen.getByRole('link');
        expect(link.getAttribute('href')).toBe('link/garage-add-manual/?type=dreamcar');
    });

    it('ex', () => {
        renderComponent(CardTypeInfo_CardType.EX_CAR);
        const link = screen.getByRole('link');
        expect(link.getAttribute('href')).toBe('link/garage-add-card/?');
    });

    it('blank', () => {
        renderComponent();
        const link = screen.getByRole('link');
        expect(link.getAttribute('href')).toBe('link/garage-add-card/?');
    });
});
