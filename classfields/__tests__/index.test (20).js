import { shallow } from 'enzyme';
import I18N from 'realty-core/view/react/libs/i18n';
import PhoneRow from '../';

const getButton = wrapper => wrapper.find('.offer-form-phone-row__button');

const getSubmitButton = wrapper => getButton(wrapper).filterWhere(
    n => n.prop('type') === 'submit'
);

const getDeleteButton = wrapper => getButton(wrapper).filterWhere(
    n => n.text() === 'Удалить телефон'
);

describe('PhoneRow', () => {
    beforeAll(() => {
        I18N.setLang('ru');
    });

    describe('with confirmation', () => {
        it("doesn't allow to submit empty phone", () => {
            const wrapper = shallow(
                <PhoneRow
                    needsConfirmation
                    phoneNumber={''}
                    isNew
                    phoneState={{}}
                />
            );

            expect(getSubmitButton(wrapper)).toHaveProp({ disabled: true });
        });

        it('shows loading state while submitting phone', () => {
            const wrapper = shallow(
                <PhoneRow
                    needsConfirmation
                    phoneNumber={'+79991112233'}
                    isNew
                    phoneState={{
                        isLoading: true
                    }}
                />
            );

            expect(getSubmitButton(wrapper)).toHaveProp({ isLoading: true });
        });

        it('lets you submit again after error', () => {
            const wrapper = shallow(
                <PhoneRow
                    phoneNumber='+79991112233'
                    needsConfirmation
                    isNew
                    phoneState={{
                        status: 'failed',
                        error: 'NONUMBER'
                    }}
                />
            );

            expect(getSubmitButton(wrapper)).toExist();
            expect(getSubmitButton(wrapper)).toHaveProp({ disabled: false });
        });

        it('lets you delete phone after code check error', () => {
            const wrapper = shallow(
                <PhoneRow
                    phoneNumber='+79991112233'
                    needsConfirmation
                    isNew
                    phoneState={{
                        status: 'pending',
                        error: 'BADCODE'
                    }}
                />
            );

            expect(getDeleteButton(wrapper)).toExist();
        });

        it("doesn't let you submit an invalid phone", () => {
            const wrapper = shallow(
                <PhoneRow
                    phoneNumber='+7999111223a'
                    needsConfirmation
                    isNew
                    phoneState={{}}
                />
            );

            expect(getSubmitButton(wrapper)).toHaveProp({ disabled: true });
        });
    });

    describe('without confirmation', () => {
        it("doesn't let you delete an empty phone", () => {
            const wrapper = shallow(
                <PhoneRow
                    phoneNumber='+7'
                    isNew
                />
            );

            expect(getDeleteButton(wrapper)).toHaveProp({ disabled: true });
        });
    });
});
