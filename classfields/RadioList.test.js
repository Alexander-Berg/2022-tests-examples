const React = require('react');
const { mount, shallow } = require('enzyme');

const RadioList = require('./RadioList');

const mockHandler = jest.fn();

const DEFAULT_PROPS = {
    list: [
        { text: 'Седан 2 дв.', val: 'SEDAN_2_DOORS' },
        { text: 'Седан', val: 'SEDAN' },
    ],
    onCheck: () => mockHandler,
    resetText: 'Не указано',
    resetOptionType: 'link',
    hasResetOption: true,
};

beforeEach(() => {
    mockHandler.mockClear();
});

describe('когда только одна опция в списке', () => {
    it('должен дергать mockHandler 1 раз при запуске', () => {
        mount(
            <RadioList { ...DEFAULT_PROPS } list={ [ DEFAULT_PROPS.list[0] ] }/>,
        );

        expect(mockHandler).toHaveBeenCalledTimes(1);
    });

    it('должен дергать mockHandler 1 раз при нажатии на "не выбрано"', () => {
        const wrapper = shallow(
            <RadioList { ...DEFAULT_PROPS } list={ [ DEFAULT_PROPS.list[0] ] }/>,
        );

        expect(mockHandler).toHaveBeenCalledTimes(1);
        wrapper.setState({ value: 'SEDAN_2_DOORS' });
        wrapper.find('.RadioList__resetLink').simulate('click');
        expect(mockHandler).toHaveBeenCalledTimes(2);
    });
});

describe('когда несколько опций в списке', () => {
    it('должен дергать mockHandler 0 раз при запуске', () => {
        mount(
            <RadioList { ...DEFAULT_PROPS }/>,
        );

        expect(mockHandler).toHaveBeenCalledTimes(0);
    });
});
