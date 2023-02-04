/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import React from 'react';

import TextInput from 'auto-core/react/components/islands/TextInput/TextInput';

import type { Props, State } from './PhoneInputWithSuggest';
import PhoneInputWithSuggest, { SUGGESTS_THEME } from './PhoneInputWithSuggest';

let props: Props;
beforeEach(() => {
    props = {
        className: 'Foo',
        size: PhoneInputWithSuggest.SIZE.L,
        name: 'phone',
        type: 'tel',
        autocomplete: TextInput.AUTOCOMPLETE.OFF,
        placeholder: 'Номер телефона',
        hasClear: true,
        disabled: false,
        error: false,
        onChange: jest.fn(),
        onFocusChange: jest.fn(),
        onClearClick: jest.fn(),
        hasInitialFocus: false,
        showSuggestForEmptyFields: true,
        initialValue: '',
        suggest: [ '79981234567', '79881234567', '79891234567' ],
        suggestsTheme: SUGGESTS_THEME.DEFAULT,
        onSelect: jest.fn(),
        defaultSuggestIndex: 0,
        width: TextInput.WIDTH.FULL,
    };
});

it('форматирует телефоны для саджеста', () => {
    const page = shallowRenderComponent({ props });
    const formattedItem = page.instance().renderSuggestItemContent({ value: props.suggest[0] });

    expect(formattedItem).toBe('+7 998 123-45-67');
});

it('при вводе номера фильтрует телефоны в саджесте', () => {
    const page = shallowRenderComponent({ props });
    return page.instance().getSuggestData('798').then((newSuggest) => {
        expect(newSuggest).toEqual([ { value: '79881234567' }, { value: '79891234567' } ]);
    });
});

it('при селекте форматирует номер телефона', () => {
    const page = shallowRenderComponent({ props });
    const formattedItem = page.instance().getClearInputOnSelect({ value: props.suggest[0] });

    expect(formattedItem).toBe('+7 998 123-45-67');
});

describe('при изменении initialValue', () => {
    it('не меняет значение, если значение изменено пользователем', () => {
        const phoneInput = shallowRenderComponent({ props });
        phoneInput.instance().onInputChange('+791229222');

        phoneInput.setProps({
            initialValue: '+791152511',
        });

        expect(phoneInput.state('inputValue')).toBe('+7 912 292-22');
    });

    it('меняет значение, если значение не изменено пользователем', () => {
        const phoneInput = shallowRenderComponent({ props });

        phoneInput.setProps({
            initialValue: '+791152511',
        });

        expect(phoneInput.state('inputValue')).toBe('+791152511');
    });
});

describe('при изменении значения в поле', () => {
    it('если значение пустое выставит в стейт дефолтное значение', () => {
        const page = shallowRenderComponent({ props });
        page.instance().onInputChange('');

        expect(page.state('inputValue')).toBe('+7');
    });

    it('если значение "+" выставит в стейт дефолтное значение', () => {
        const page = shallowRenderComponent({ props });
        page.instance().onInputChange('+');

        expect(page.state('inputValue')).toBe('+7');
    });

    describe('если значение не пустое', () => {
        let page: ShallowWrapper<Props, State, PhoneInputWithSuggest>;
        beforeEach(() => {
            page = shallowRenderComponent({ props });
            page.instance().onInputChange('+790935133');
        });

        it('отформатирует его и положит в стейт', () => {
            expect(page.state('inputValue')).toBe('+7 909 351-33');
        });

        it('вызовет проп и передаст в него необходимые параметры', () => {
            expect(props.onChange).toHaveBeenCalledTimes(1);
            expect(props.onChange).toHaveBeenCalledWith('+7 909 351-33', props, { isFormatted: true });
        });
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const page = shallow<PhoneInputWithSuggest, Props, State>(
        <PhoneInputWithSuggest { ...props }/>,
    );

    return page;
}
