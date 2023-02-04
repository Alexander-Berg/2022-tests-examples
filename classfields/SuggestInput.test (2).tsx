import React from 'react';
import _ from 'lodash';

import { shallow } from 'enzyme';

import SuggestInput, { SuggestItem } from './SuggestInput';

const KEY_DOWN_EVT = {
    preventDefault: _.noop,
    keyCode: 40,
} as KeyboardEvent;

const KEY_UP_EVT = {
    preventDefault: _.noop,
    keyCode: 38,
} as KeyboardEvent;


const SUGGEST_DATA: Array<SuggestItem> = Array(10).fill({ value: '' });

const get_suggest_data = (): Promise<Array<SuggestItem>> => Promise.resolve(SUGGEST_DATA);
const render_suggest_item = (item: SuggestItem): JSX.Element => <div> { item.value } </div>;

describe('обработка нажатия кнопок', () => {
    let tree;
    let instance: SuggestInput;

    beforeEach(() => {
        tree = shallow(
            <SuggestInput
                render_suggest_item={ render_suggest_item }
                get_suggest_data={ get_suggest_data }
            />,
        );
        instance = tree.instance() as SuggestInput;
    });

    it('должен правильно обработать первое нажатие кнопки "стрелочка вниз"', () => {
        instance.setState({
            suggest_data: SUGGEST_DATA,
        });
        expect(instance.state.suggest_index).toBeNull();
        instance.on_input_key_down(KEY_DOWN_EVT);
        expect(instance.state.suggest_index).toBe(0);
    });

    it('должен правильно обработать второе и последующие нажатия кнопки "стрелочка вниз"', () => {
        instance.setState({
            suggest_index: 0,
            suggest_data: SUGGEST_DATA,
        });
        instance.on_input_key_down(KEY_DOWN_EVT);
        expect(instance.state.suggest_index).toBe(1);
    });

    it('должен правильно обработать нажатие кнопки "стрелочка вниз", когда выбран последний элемент списка', () => {
        instance.setState({
            suggest_index: 9,
            suggest_data: SUGGEST_DATA,
        });
        instance.on_input_key_down(KEY_DOWN_EVT);
        expect(instance.state.suggest_index).toBe(0);
    });

    it('должен правильно обработать первое нажатие кнопки "стрелочка вверх"', () => {
        instance.setState({
            suggest_data: SUGGEST_DATA,
        });
        expect(instance.state.suggest_index).toBeNull();
        instance.on_input_key_down(KEY_UP_EVT);
        expect(instance.state.suggest_index).toBe(9);
    });

    it('должен правильно обработать второе и последующие нажатия кнопки "стрелочка вверх"', () => {
        instance.setState({
            suggest_index: 9,
            suggest_data: SUGGEST_DATA,
        });
        instance.on_input_key_down(KEY_UP_EVT);
        expect(instance.state.suggest_index).toBe(8);
    });

    it('должен правильно обработать нажатие кнопки "стрелочка вверх", когда выбран первый элемент списка', () => {
        instance.setState({
            suggest_index: 0,
            suggest_data: SUGGEST_DATA,
        });
        instance.on_input_key_down(KEY_UP_EVT);
        expect(instance.state.suggest_index).toBe(9);
    });
});
