/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const RichInput = require('./RichInput').default;

const KEY_DOWN_EVT = {
    preventDefault: _.noop,
    keyCode: 40,
};

const KEY_UP_EVT = {
    preventDefault: _.noop,
    keyCode: 38,
};

const SUGGEST_DATA = Array(10).fill({});

describe('обработка нажатия кнопок', () => {
    let tree;
    let instance;

    beforeEach(() => {
        tree = shallow(
            <RichInput/>,
        );
        instance = tree.instance();
    });

    it('должен правильно обработать первое нажатие кнопки "стрелочка вниз"', () => {
        instance.setState({
            suggestData: SUGGEST_DATA,
        });
        expect(instance.state.suggestIndex).toBeUndefined();
        instance.onInputKeyDown(KEY_DOWN_EVT);
        expect(instance.state.suggestIndex).toBe(0);
    });

    it('должен правильно обработать второе и последующие нажатия кнопки "стрелочка вниз"', () => {
        instance.setState({
            suggestIndex: 0,
            suggestData: SUGGEST_DATA,
        });
        instance.onInputKeyDown(KEY_DOWN_EVT);
        expect(instance.state.suggestIndex).toBe(1);
    });

    it('должен правильно обработать нажатие кнопки "стрелочка вниз", когда выбран последний элемент списка', () => {
        instance.setState({
            suggestIndex: 9,
            suggestData: SUGGEST_DATA,
        });
        instance.onInputKeyDown(KEY_DOWN_EVT);
        expect(instance.state.suggestIndex).toBe(0);
    });

    it('должен правильно обработать первое нажатие кнопки "стрелочка вверх"', () => {
        instance.setState({
            suggestData: SUGGEST_DATA,
        });
        expect(instance.state.suggestIndex).toBeUndefined();
        instance.onInputKeyDown(KEY_UP_EVT);
        expect(instance.state.suggestIndex).toBe(9);
    });

    it('должен правильно обработать второе и последующие нажатия кнопки "стрелочка вверх"', () => {
        instance.setState({
            suggestIndex: 9,
            suggestData: SUGGEST_DATA,
        });
        instance.onInputKeyDown(KEY_UP_EVT);
        expect(instance.state.suggestIndex).toBe(8);
    });

    it('должен правильно обработать нажатие кнопки "стрелочка вверх", когда выбран первый элемент списка', () => {
        instance.setState({
            suggestIndex: 0,
            suggestData: SUGGEST_DATA,
        });
        instance.onInputKeyDown(KEY_UP_EVT);
        expect(instance.state.suggestIndex).toBe(9);
    });
});
