import React from 'react';
import { shallow } from 'enzyme';

import { BuyerStep, ParticipantType } from '@vertis/schema-registry/ts-types-snake/vertis/safe_deal/common';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import TextInput from 'auto-core/react/components/islands/TextInput/TextInput';

import type { Props as AbstractProps, State } from './DealFormAbstract';
import DealFormAbstract from './DealFormAbstract';

const Context = createContextProvider(contextMock);

it('НЕ должен вызывать onUpdate при submit если обязательное поле НЕ заполнено', () => {
    const update = jest.fn(() => Promise.resolve());
    const tree = renderWrapper(update).dive();

    const submitButton = tree.find('.PageDealForm__button');
    submitButton.simulate('click');

    expect(update).toHaveBeenCalledTimes(0);
});

it('должен пробросить ошибку в input при submit если обязательное поле НЕ заполнено', () => {
    const update = jest.fn(() => Promise.resolve());
    const tree = renderWrapper(update).dive();

    const submitButton = tree.find('.PageDealForm__button');
    submitButton.simulate('click');

    const input = tree.find('TextInput');

    expect(input.prop('error')).toBe(true);
});

it('должен вызывать onUpdate при submit если обязательное поле заполнено', () => {
    const update = jest.fn(() => Promise.resolve());
    const wrapper = renderWrapper(update);
    const tree = wrapper.dive();

    const input = tree.find('TextInput');
    input.simulate('change', 'abc');

    const submitButton = tree.find('.PageDealForm__button');
    submitButton.simulate('click');

    expect(update).toHaveBeenCalledTimes(1);
});

type FormField = 'input';

type Props = AbstractProps;

class Form extends DealFormAbstract<FormField, Props, State> {
    getApiData = jest.fn();
    getApiErrors = jest.fn();

    render() {
        return (
            <div>
                <TextInput
                    { ...this.getProps('input') }
                />
                { this.renderButtons() }
            </div>
        );
    }
}

function renderWrapper(update: jest.Mock) {
    return shallow(
        <Context>
            <Form
                formData={{ input: '' }}
                onUpdate={ update }
                dealId="12345"
                step={ BuyerStep.BUYER_INTRODUCING_PASSPORT_DETAILS }
                formStep="passport"
                role={ ParticipantType.BUYER }
                dealPageType={ ParticipantType.BUYER.toLowerCase() }
            />
        </Context>,
    );
}
