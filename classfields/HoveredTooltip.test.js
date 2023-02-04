/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { mount } = require('enzyme');

const HoveredTooltip = require('./HoveredTooltip');

describe('onClick', () => {
    it('Должен вызвать onClick при клике по кнопке, если у кнопки есть onClick', () => {
        const onClick = jest.fn();
        const tooltipContent = <div className="TooltipContent">Контент</div>;
        const hoveredTooltip = mount(
            <HoveredTooltip
                isVisible={ false }
                tooltipContent={ tooltipContent }
            >
                <span className="Button" onClick={ onClick }>Кнопка</span>
            </HoveredTooltip>);
        hoveredTooltip.find('.Button').simulate('click');
        expect(onClick).toHaveBeenCalled();
    });

    it('Должен вызвать onClick при клике по контенту тултипа, если у кнопки есть onClick', () => {
        const onClick = jest.fn();
        const tooltipContent = <div className="TooltipContent">Контент</div>;
        const hoveredTooltip = mount(
            <HoveredTooltip
                isVisible={ true }
                tooltipContent={ tooltipContent }
            >
                <span className="Button" onClick={ onClick }>Кнопка</span>
            </HoveredTooltip>);
        hoveredTooltip.find('.TooltipContent').simulate('click');
        expect(onClick).toHaveBeenCalled();
    });

    it('Не должен вызвать onClick при клике по контенту тултипа, если кнопка обернута во wrapper', () => {
        const onClick = jest.fn();
        const tooltipContent = <div className="TooltipContent">Контент</div>;
        const hoveredTooltip = mount(
            <HoveredTooltip
                isVisible={ true }
                tooltipContent={ tooltipContent }
            >
                <div className="ButtonWrapper">
                    <span
                        className="Button"
                        onClick={ onClick }
                    >Кнопка</span>
                </div>
            </HoveredTooltip>);
        hoveredTooltip.find('.TooltipContent').simulate('click');
        expect(onClick).not.toHaveBeenCalled();
    });
});
