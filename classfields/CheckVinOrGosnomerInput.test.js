/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
jest.mock('auto-core/fields/vin/vin-validator');
jest.mock('auto-core/fields/gosnomer/gosnomer-validator');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const CheckVinOrGosnomerInput = require('./CheckVinOrGosnomerInput');

global.open = jest.fn();

it('должен вернуть корректный компонент', () => {
    expect(shallowToJson(shallow(
        <CheckVinOrGosnomerInput/>,
        { context: contextMock },
    ))).toMatchSnapshot();
});

it('componentDidMount тест: должен вызвать this.input.focus', () => {
    const focus = jest.fn();
    const CheckVinOrGosnomerInputState = shallow(
        <CheckVinOrGosnomerInput/>,
        { context: contextMock },
    ).instance();
    CheckVinOrGosnomerInputState.input = {
        focus,
    };
    CheckVinOrGosnomerInputState.componentDidMount();

    expect(focus).toHaveBeenCalled();
});

it('onVinOrGosnomerInputKeyDown тест: должен вызвать onSubmitVinOrGosnomer, если keyCode === 13', () => {
    const onSubmitVinOrGosnomer = jest.fn();

    const CheckVinOrGosnomerInputState = shallow(
        <CheckVinOrGosnomerInput/>,
        { context: contextMock },
    ).instance();

    CheckVinOrGosnomerInputState.onSubmitVinOrGosnomer = onSubmitVinOrGosnomer;
    CheckVinOrGosnomerInputState.onVinOrGosnomerInputKeyDown({ keyCode: 13 });

    expect(onSubmitVinOrGosnomer).toHaveBeenCalled();
});

it('onVinOrGosnomerChange тест: должен вызвать setState с корректными параметрами', () => {
    const setState = jest.fn();

    const CheckVinOrGosnomerInputState = shallow(
        <CheckVinOrGosnomerInput/>,
        { context: contextMock },
    ).instance();

    CheckVinOrGosnomerInputState.setState = setState;
    CheckVinOrGosnomerInputState.onVinOrGosnomerChange('у893км750');

    expect(setState).toHaveBeenCalledWith({
        value: 'Y893KM750',
        error: '',
    });
});

describe('onSubmitVinOrGosnomer', () => {
    it('должен вызвать metrika.sendPageEvent с корректными параметрами', () => {
        const CheckVinOrGosnomerInputState = shallow(
            <CheckVinOrGosnomerInput/>,
            { context: contextMock },
        ).instance();

        CheckVinOrGosnomerInputState.onSubmitVinOrGosnomer();

        expect(contextMock.metrika.params).toHaveBeenCalledWith([ 'check_vin-button-click' ]);
    });

    it('должен вызвать window.open с корректными параметрами, если vinValidator', () => {
        require('auto-core/fields/vin/vin-validator').mockImplementation(() => true);
        require('auto-core/fields/gosnomer/gosnomer-validator').mockImplementation(() => false);

        const CheckVinOrGosnomerInputState = shallow(
            <CheckVinOrGosnomerInput/>,
            { context: contextMock },
        ).instance();

        CheckVinOrGosnomerInputState.onSubmitVinOrGosnomer();
        expect(global.open).toHaveBeenCalledWith('link/proauto-report/?history_entity_id=', '_blank');
    });

    it('должен вызвать window.open с корректными параметрами, если gosnomerValidator', () => {
        require('auto-core/fields/vin/vin-validator').mockImplementation(() => false);
        require('auto-core/fields/gosnomer/gosnomer-validator').mockImplementation(() => true);

        const CheckVinOrGosnomerInputState = shallow(
            <CheckVinOrGosnomerInput/>,
            { context: contextMock },
        ).instance();

        CheckVinOrGosnomerInputState.onSubmitVinOrGosnomer();
        expect(global.open).toHaveBeenCalledWith('link/proauto-report/?history_entity_id=', '_blank');
    });

    it('должен вызвать setState с корректными параметрами, если !vinValidator && !gosnomerValidator', () => {
        const setState = jest.fn();
        require('auto-core/fields/vin/vin-validator').mockImplementation(() => false);
        require('auto-core/fields/gosnomer/gosnomer-validator').mockImplementation(() => false);

        const CheckVinOrGosnomerInputState = shallow(
            <CheckVinOrGosnomerInput/>,
            { context: contextMock },
        ).instance();

        CheckVinOrGosnomerInputState.setState = setState;
        CheckVinOrGosnomerInputState.onSubmitVinOrGosnomer();
        expect(setState).toHaveBeenCalledWith({
            error: 'Введите правильный VIN/госномер',
        });
    });
});
