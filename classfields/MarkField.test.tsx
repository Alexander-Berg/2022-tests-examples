import React from 'react';
import { within } from '@testing-library/dom';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import markMock from 'auto-core/models/catalogSuggest/mocks/mark.mock';

import type { Props } from './MarkField';
import MarkField from './MarkField';

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        onSelect: jest.fn(),
        fetchCatalogSuggest: jest.fn(),
        isCatalogSuggestError: false,
        isCatalogSuggestFetching: false,
    };
});

it('при клике на "еще" откроет модал', async() => {
    const props = {
        ...defaultProps,
        marks: [
            markMock.withId('AUDI').withName('Audi').value(),
            markMock.withId('BMW').withName('BMW').value(),
            markMock.withId('KIA').withName('Kia').value(),
            markMock.withId('FERRARI').withName('Ferrari').value(),
            markMock.withId('NIVA').withName('Niva').value(),
        ],
    };
    const { getByRole, queryByRole } = await renderComponent(<MarkField { ...props }/>);
    const cutLink = await getByRole('button', { name: 'Все марки' });

    let modal = await queryByRole('dialog');
    expect(modal).toBeNull();

    userEvent.click(cutLink);

    modal = await getByRole('dialog');
    const inputInModal = await within(modal).getByLabelText('Марка');
    expect(inputInModal).not.toBeNull();
});

it('при выборе марки в модале, подставит ее и закроет модал', async() => {
    const props = {
        ...defaultProps,
        marks: [
            markMock.withId('AUDI').withName('Audi').value(),
            markMock.withId('BMW').withName('BMW').value(),
            markMock.withId('KIA').withName('Kia').value(),
            markMock.withId('FERRARI').withName('Ferrari').value(),
            markMock.withId('NIVA').withName('Niva').value(),
        ],
    };
    const { findByLabelText, getByRole, queryByRole } = await renderComponent(<MarkField { ...props }/>);
    const cutLink = await getByRole('button', { name: 'Все марки' });

    userEvent.click(cutLink);

    let modal: HTMLElement | null = await getByRole('dialog');
    const item = await within(modal).getByRole('button', { name: 'Audi' });

    userEvent.click(item);

    const input = await findByLabelText('Марка') as HTMLInputElement;
    expect(input?.value).toBe('Audi');

    expect(props.onSelect).toHaveBeenCalledTimes(1);
    expect(props.onSelect).toHaveBeenCalledWith({ data: 'AUDI', text: 'Audi' });

    modal = await queryByRole('dialog');
    expect(modal).toBeNull();
});
