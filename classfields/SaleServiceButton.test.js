const React = require('react');
const { shallow } = require('enzyme');
const SaleServiceButton = require('./SaleServiceButton');
const _ = require('lodash');

const baseProps = {
    canWriteSaleResource: true,
    iconId: 'service-turbo',
    service: 'turbo',
    text: 'text',
    onClick: _.noop,
};

it('render тест: должен вернуть корректный элемент', () => {
    const SaleServiceButtonInstance = shallow(
        <SaleServiceButton
            { ...baseProps }
            active={ true }
            disabled={ true }
            isFetching={ true }
            deactivationAllowed={ false }
        />,
    ).instance();

    SaleServiceButtonInstance.getDeactivationNotAllowed = () => true;
    SaleServiceButtonInstance.renderIcon = () => 'Icon';

    expect(SaleServiceButtonInstance.render()).toMatchSnapshot();
});

describe('onClick тесты:', () => {
    it('должен вызвать onClick, если disabled, isFetching, deactivationNotAllowed = false', () => {
        const onClick = jest.fn();
        const SaleServiceButtonInstance = shallow(
            <SaleServiceButton
                { ...baseProps }
                onClick={ onClick }
                disabled={ false }
                isFetching={ false }
            />,
        ).instance();

        SaleServiceButtonInstance.getDeactivationNotAllowed = () => false;

        SaleServiceButtonInstance.onClick();

        expect(onClick).toHaveBeenCalled();
    });

    it('не должен вызвать onClick, если disabled = true', () => {
        const onClick = jest.fn();
        const SaleServiceButtonInstance = shallow(
            <SaleServiceButton
                { ...baseProps }
                onClick={ onClick }
                disabled={ true }
                isFetching={ false }
            />,
        ).instance();

        SaleServiceButtonInstance.getDeactivationNotAllowed = () => false;

        SaleServiceButtonInstance.onClick();

        expect(onClick).not.toHaveBeenCalled();
    });

    it('не должен вызвать onClick, если isFetching = true', () => {
        const onClick = jest.fn();
        const SaleServiceButtonInstance = shallow(
            <SaleServiceButton
                { ...baseProps }
                onClick={ onClick }
                disabled={ false }
                isFetching={ true }
            />,
        ).instance();

        SaleServiceButtonInstance.getDeactivationNotAllowed = () => false;

        SaleServiceButtonInstance.onClick();

        expect(onClick).not.toHaveBeenCalled();
    });

    it('не должен вызвать onClick, если deactivationNotAllowed = true', () => {
        const onClick = jest.fn();
        const SaleServiceButtonInstance = shallow(
            <SaleServiceButton
                { ...baseProps }
                onClick={ onClick }
                disabled={ false }
                isFetching={ false }
            />,
        ).instance();

        SaleServiceButtonInstance.getDeactivationNotAllowed = () => true;

        SaleServiceButtonInstance.onClick();

        expect(onClick).not.toHaveBeenCalled();
    });
});

describe('renderIcon тесты', () => {
    it('должен вернуть элемент с SaleServiceButton__indicatorWrapper', () => {
        const SaleServiceButtonInstance = shallow(
            <SaleServiceButton
                { ...baseProps }
                disabled={ false }
                isFetching={ false }
                indicator={ 10 }
            />,
        ).instance();

        expect(SaleServiceButtonInstance.renderIcon()).toMatchSnapshot();
    });

    it('должен вернуть элемент со спиннером, если isFetching = true', () => {
        const SaleServiceButtonInstance = shallow(
            <SaleServiceButton
                { ...baseProps }
                disabled={ false }
                isFetching={ true }
            />,
        ).instance();

        expect(SaleServiceButtonInstance.renderIcon()).toMatchSnapshot();
    });

    it('должен вернуть корректный элемент', () => {
        const SaleServiceButtonInstance = shallow(
            <SaleServiceButton
                { ...baseProps }
                isFetching={ false }
            />,
        ).instance();

        expect(SaleServiceButtonInstance.renderIcon()).toMatchSnapshot();
    });
});

describe('getDeactivationNotAllowed тесты', () => {
    it('должен вернуть true, если service != fresh && deactivationAllowed === false', () => {
        const SaleServiceButtonInstance = shallow(
            <SaleServiceButton
                { ...baseProps }
                deactivationAllowed={ false }
            />,
        ).instance();

        expect(SaleServiceButtonInstance.getDeactivationNotAllowed()).toBe(true);
    });

    it('должен вернуть false, если service === fresh', () => {
        const SaleServiceButtonInstance = shallow(
            <SaleServiceButton
                text="text"
                iconId="fresh"
                service="fresh"
            />,
        ).instance();

        expect(SaleServiceButtonInstance.getDeactivationNotAllowed()).toBe(false);
    });

    it('должен вернуть false, если service !== fresh && deactivationAllowed = true', () => {
        const SaleServiceButtonInstance = shallow(
            <SaleServiceButton
                { ...baseProps }
                service="turbo"
                deactivationAllowed={ true }
            />,
        ).instance();

        expect(SaleServiceButtonInstance.getDeactivationNotAllowed()).toBe(false);
    });
});
