import { shallow } from 'enzyme';
import { FormPhoneComponent } from '../';
import PhoneRow from '../components/phone-row';

const ownerData = {
    userType: 'OWNER',
    // Owner
    telephones: [ '+79991112233' ],
    availableTelephones: [ '+79991112233' ],
    // Agency
    agencyTelephones: [],
    availableAgencyTelephones: []
};

const ownerProps = {
    name: 'telephones',
    availableName: 'availableTelephones',
    data: ownerData,
    phoneState: {},
    crc: '',
    actions: {},
    phoneActions: {}
};

const getAddPhoneButton = wrapper => wrapper.find('.offer-form-phone__add');

describe('FormPhone', () => {
    describe('OWNER', () => {
        it("doesn't show add new phone button when duplicate phone is entered", () => {
            const wrapper = shallow(
                <FormPhoneComponent
                    {...ownerProps}
                />
            );

            getAddPhoneButton(wrapper).simulate('click');

            const phoneRow = wrapper.find(PhoneRow).last();

            phoneRow.simulate('phoneChange', '+79991112233', 1);

            expect(getAddPhoneButton(wrapper)).not.toExist();
        });
    });

    it('switches phones when changing from owner to agency', () => {
        const wrapper = shallow(
            <FormPhoneComponent
                {...ownerProps}
            />
        );

        expect(wrapper.find(PhoneRow)).toHaveProp({ phoneNumber: '+79991112233' });

        wrapper.setProps({
            data: {
                ...ownerData,
                userType: 'AGENCY'
            },
            name: 'agencyTelephones',
            availableName: 'availableAgencyTelephones'
        });

        expect(wrapper.find(PhoneRow)).toHaveProp({ phoneNumber: '' });
    });

    it('keeps phones when changing from owner to agent', () => {
        const wrapper = shallow(
            <FormPhoneComponent
                {...ownerProps}
            />
        );

        wrapper.setProps({
            data: {
                ...ownerData,
                userType: 'AGENT'
            }
        });

        expect(wrapper.find(PhoneRow)).toHaveProp({ phoneNumber: '+79991112233' });
    });
});
