const React = require('react');
const { shallow } = require('enzyme');

const Item = require('../Item');
const Tags = require('./Tags');

describe('mode=CHECK', () => {
    it('не должен зачекать первое значение, если не передали value', () => {
        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.CHECK }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
                <Item value="value3">value3</Item>
            </Tags>,
        );

        expect(wrapper.find('Button[checked=true]')).toHaveLength(0);
    });

    it('должен зачекать переданное значение', () => {
        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.CHECK }
                value={ [ 'value1', 'value3' ] }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
                <Item value="value3">value3</Item>
            </Tags>,
        );

        expect(wrapper.find('Button[value="value1"]')).toHaveProp('checked', true);
        expect(wrapper.find('Button[value="value3"]')).toHaveProp('checked', true);
    });

    it('должен вызвать onChange после клика', () => {
        const fn = jest.fn();

        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.CHECK }
                onChange={ fn }
                value={ [ 'value2' ] }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
                <Item value="value3">value3</Item>
            </Tags>,
        );
        wrapper.find({ value: 'value1' }).simulate('click', null, { value: 'value1' });

        expect(fn).toHaveBeenCalledTimes(1);
        expect(fn.mock.calls[0][0]).toEqual([ 'value2', 'value1' ]);
    });

    it('должен вызвать onChange после клика в уже выбранное значение', () => {
        const fn = jest.fn();

        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.CHECK }
                onChange={ fn }
                value={ [ 'value1', 'value3' ] }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
                <Item value="value3">value3</Item>
            </Tags>,
        );
        wrapper.find({ value: 'value3' }).simulate('click', null, { value: 'value3' });

        expect(fn).toHaveBeenCalledTimes(1);
        expect(fn.mock.calls[0][0]).toEqual([ 'value1' ]);
    });
});

describe('mode=RADIO', () => {
    it('должен зачекать первое значение, если не передали value', () => {
        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
            </Tags>,
        );

        expect(wrapper.find('Button[value="value1"]')).toHaveProp('checked', true);
    });

    it('должен зачекать переданное значение', () => {
        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO }
                value={ [ 'value2' ] }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
            </Tags>,
        );

        expect(wrapper.find('Button[value="value2"]')).toHaveProp('checked', true);
    });

    it('должен зачекать значение false', () => {
        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO }
                value={ [ false ] }
            >
                <Item value={ true }>value1</Item>
                <Item value={ false }>value2</Item>
            </Tags>,
        );

        expect(wrapper.find('Button').at(1)).toHaveProp('checked', true);
    });

    it('должен вызвать onChange с false', () => {
        const fn = jest.fn();

        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO }
                onChange={ fn }
            >
                <Item value={ true }>value1</Item>
                <Item value={ false }>value2</Item>
            </Tags>,
        );

        wrapper.find({ value: false }).simulate('click', null, { value: false });

        expect(fn).toHaveBeenCalledTimes(1);
        expect(fn.mock.calls[0][0]).toEqual([ false ]);
    });

    it('должен вызвать onChange после клика', () => {
        const fn = jest.fn();

        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO }
                onChange={ fn }
                value={ [ 'value2' ] }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
            </Tags>,
        );
        wrapper.find({ value: 'value1' }).simulate('click', null, { value: 'value1' });

        expect(fn).toHaveBeenCalledTimes(1);
        expect(fn.mock.calls[0][0]).toEqual([ 'value1' ]);
    });

    it('не должен вызвать onChange после клика в уже выбранное значение', () => {
        const fn = jest.fn();

        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO }
                onChange={ fn }
                value={ [ 'value2' ] }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
            </Tags>,
        );
        wrapper.find({ value: 'value2' }).simulate('click', null, { value: 'value2' });

        expect(fn).toHaveBeenCalledTimes(0);
    });

    it('не должен вызвать onChange после клика в уже выбранное значение false', () => {
        const fn = jest.fn();

        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO }
                onChange={ fn }
                value={ [ false ] }
            >
                <Item value="value1">value1</Item>
                <Item value={ false }>value2</Item>
            </Tags>,
        );
        wrapper.find({ value: false }).simulate('click', null, { value: false });

        expect(fn).toHaveBeenCalledTimes(0);
    });
});

describe('mode=RADIO_CHECK', () => {
    it('не должен зачекать первое значение, если не передали value', () => {
        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO_CHECK }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
            </Tags>,
        );

        expect(wrapper.find('Button[checked=true]')).toHaveLength(0);
    });

    it('должен зачекать переданное значение', () => {
        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO_CHECK }
                value={ [ 'value2' ] }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
            </Tags>,
        );

        expect(wrapper.find('Button[value="value2"]')).toHaveProp('checked', true);
    });

    it('должен зачекать значение false', () => {
        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO_CHECK }
                value={ [ false ] }
            >
                <Item value={ true }>value1</Item>
                <Item value={ false }>value2</Item>
            </Tags>,
        );

        expect(wrapper.find('Button').at(1)).toHaveProp('checked', true);
    });

    it('должен вызвать onChange с false', () => {
        const fn = jest.fn();

        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO_CHECK }
                onChange={ fn }
            >
                <Item value={ true }>value1</Item>
                <Item value={ false }>value2</Item>
            </Tags>,
        );

        wrapper.find({ value: false }).simulate('click', null, { value: false });

        expect(fn).toHaveBeenCalledTimes(1);
        expect(fn.mock.calls[0][0]).toEqual([ false ]);
    });

    it('должен вызвать onChange после клика', () => {
        const fn = jest.fn();

        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO }
                onChange={ fn }
                value={ [ 'value2' ] }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
            </Tags>,
        );

        wrapper.find({ value: 'value1' }).simulate('click', null, { value: 'value1' });

        expect(fn).toHaveBeenCalledTimes(1);
        expect(fn.mock.calls[0][0]).toEqual([ 'value1' ]);
    });

    it('должен вызвать onChange с пустым значением после клика в уже выбранное значение', () => {
        const fn = jest.fn();

        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO_CHECK }
                onChange={ fn }
                value={ [ 'value2' ] }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
            </Tags>,
        );
        wrapper.find({ value: 'value2' }).simulate('click', null, { value: 'value2' });

        expect(fn).toHaveBeenCalledTimes(1);
        expect(fn.mock.calls[0][0]).toEqual([ ]);
    });

    it('должен вызвать onChange с пустым значением после клика в уже выбранное значение false', () => {
        const fn = jest.fn();

        const wrapper = shallow(
            <Tags
                mode={ Tags.MODE.RADIO_CHECK }
                onChange={ fn }
                value={ [ false ] }
            >
                <Item value="value1">value1</Item>
                <Item value="value2">value2</Item>
                <Item value={ false }>value3</Item>
            </Tags>,
        );
        wrapper.find({ value: false }).simulate('click', null, { value: false });

        expect(fn).toHaveBeenCalledTimes(1);
        expect(fn.mock.calls[0][0]).toEqual([ ]);
    });
});

it('не должен рендерить falsy чилдренов', () => {
    const wrapper = shallow(
        <Tags
            mode={ Tags.MODE.RADIO_CHECK }
            value={ [ 'value2' ] }
        >
            <Item value="value1">value1</Item>
            { null }
            { false }
            { undefined }
            <Item value="value2">value2</Item>
        </Tags>,
    );

    expect(wrapper.children()).toHaveLength(2);
});
