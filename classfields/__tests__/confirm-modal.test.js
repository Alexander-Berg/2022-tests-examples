import * as React from 'react';
import { shallow } from 'enzyme';
import ConfirmModal from '../';

const commonProps = {
    message: '',
    continueText: '',
    cancelText: '',
    visible: true,
    onCancel: jest.fn(),
    onContinue: jest.fn()
};

describe('ConfirmModal', () => {
    it('handles cancel click', () => {
        const handleCancel = jest.fn();
        const wrapper = shallow(
            <ConfirmModal
                {...commonProps}
                onCancel={handleCancel}
            />
        );
        const cancelButton = wrapper.find('.confirm-modal__control_type_cancel');

        expect(handleCancel).toHaveBeenCalledTimes(0);
        cancelButton.simulate('click');
        expect(handleCancel).toHaveBeenCalledTimes(1);
    });

    it('handles continue click', () => {
        const handleContinue = jest.fn();
        const wrapper = shallow(
            <ConfirmModal
                {...commonProps}
                onContinue={handleContinue}
            />
        );
        const continueButton = wrapper.find('.confirm-modal__control_type_continue');

        expect(handleContinue).toHaveBeenCalledTimes(0);
        continueButton.simulate('click');
        expect(handleContinue).toHaveBeenCalledTimes(1);
    });
});
