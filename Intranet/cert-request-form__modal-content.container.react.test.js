import React from 'react';
import CertRequestForm from 'b:cert-request-form e:modal-content.container';
import {mount} from 'enzyme';

describe('cert-request-form__modal-content.container', () => {
    let data;

    beforeEach(() => {
        data = {
            certRequestForm: {
                additionalFieldsConfig: []
            },
            requestAdditionalFieldsConfig: jest.fn(),
            onCancelButtonClick: jest.fn(),
            initialFieldsValues: {},
            path: '/certificates?ca-form=1',
            filterQueryStr: jest.fn()
        };
    });

    it('should call checkForLoadAdditionalFields on didMount', () => {
        const spy = jest.spyOn(CertRequestForm.prototype, 'checkForLoadAdditionalFields');
        const wrapper = mount(
            <CertRequestForm {...data} />
        );

        expect(spy).toHaveBeenCalled();

        wrapper.unmount();
    });

    it('should call clearQueryParams on didMount', () => {
        const spy = jest.spyOn(CertRequestForm.prototype, 'clearQueryParams');
        const wrapper = mount(
            <CertRequestForm {...data} />
        );

        expect(spy).toHaveBeenCalled();

        wrapper.unmount();
    });

    it('should be called on issue button click if issue button is available', () => {
        const spy = jest.spyOn(CertRequestForm.prototype, 'handleIssueButtonClick');

        const wrapper = mount(
            <CertRequestForm {...data} />
        );

        wrapper.setState({
            isIssueButtonAvailable: true
        });

        wrapper.find('.cert-request-form__button').at(0).simulate('click');

        expect(spy).toHaveBeenCalled();

        wrapper.unmount();
    });

    it('should not be called on click if issue button is not available', () => {
        const spy = jest.spyOn(CertRequestForm.prototype, 'handleIssueButtonClick');

        const wrapper = mount(
            <CertRequestForm {...data} />
        );

        wrapper.update();

        wrapper.find('.cert-request-form__button').at(0).simulate('click');

        expect(spy).toHaveBeenCalled();

        wrapper.unmount();
    });
});
