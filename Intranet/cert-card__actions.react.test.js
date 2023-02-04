import React from 'react';
import Actions from 'b:cert-card e:actions';
import {mount} from 'enzyme';

describe('Action button', () => {
    it('should contain a proper content', () => {
        const wrapper = mount(
            <Actions
                cert={{
                    id: 11,
                    available_actions: [
                        {
                            id: 'unhold',
                            name: {
                                ru: 'Разморозить'
                            }
                        }
                    ]
                }}
                actionError={{
                    id: '',
                    text: ''
                }}
                isActionInProgress={false}
                onTooltipOutsideClick={jest.fn()}
                onActionClick={jest.fn()}
                isAdditionalFieldsConfigLoading={false}
                onReissueButtonClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('should show an error tooltip', () => {
        const wrapper = mount(
            <Actions
                cert={{
                    id: 11,
                    available_actions: [
                        {
                            id: 'unhold',
                            name: {
                                ru: 'Разморозить'
                            }
                        }
                    ]
                }}
                actionError={{
                    id: 'unhold',
                    text: 'error-text'
                }}
                isActionInProgress={false}
                onTooltipOutsideClick={jest.fn()}
                onActionClick={jest.fn()}
                isAdditionalFieldsConfigLoading={false}
                onReissueButtonClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('should handle a button click', () => {
        const clickHandler = jest.fn();
        const wrapper = mount(
            <Actions
                cert={{
                    id: 11,
                    available_actions: [
                        {
                            id: 'unhold',
                            name: {
                                ru: 'Разморозить'
                            }
                        }
                    ]
                }}
                actionError={{
                    id: '',
                    text: 'error-text'
                }}
                isActionInProgress={false}
                onTooltipOutsideClick={jest.fn()}
                onActionClick={clickHandler}
                isAdditionalFieldsConfigLoading={false}
                onReissueButtonClick={jest.fn()}
            />
        );

        wrapper.find('.cert-card__button').simulate('click');

        expect(clickHandler).toHaveBeenCalledWith(11, 'unhold');

        wrapper.unmount();
    });
});

describe('Reissue button', () => {
    it('should contain a proper content', () => {
        const wrapper = mount(
            <Actions
                cert={{
                    id: 11,
                    available_actions: [],
                    type: 'host'
                }}
                actionError={{
                    id: '',
                    text: ''
                }}
                isActionInProgress={false}
                isAdditionalFieldsConfigLoading={false}
                additionalFieldsConfigError=""
                onTooltipOutsideClick={jest.fn()}
                onActionClick={jest.fn()}
                onReissueButtonClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('should show an error tooltip', () => {
        const wrapper = mount(
            <Actions
                cert={{
                    id: 11,
                    available_actions: [],
                    type: 'host'
                }}
                actionError={{
                    id: '',
                    text: ''
                }}
                isActionInProgress={false}
                isAdditionalFieldsConfigLoading={false}
                additionalFieldsConfigError="Some error"
                onTooltipOutsideClick={jest.fn()}
                onActionClick={jest.fn()}
                onReissueButtonClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });
});
