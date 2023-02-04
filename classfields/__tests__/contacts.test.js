import { shallow } from 'enzyme';

import { ContactsForm } from '..';

const actions = {};

const data = {
    hasAccount: false,
    telephones: [],
    agencyTelephones: [],
    availableTelephones: [],
    availableAgencyTelephones: [],
    _errors: {},
    userType: 'AGENCY',
    formType: 'offer',
    redirectPhones: false
};

const commonProps = {
    data,
    actions,
    isAuth: true,
    isJuridical: false,
    phoneError: '',
    phoneStatus: '',
    redirectPhones: false
};

const propsWithoutVos = {
    ...commonProps,
    hasOgrn: false,
    isVosUser: false
};

const propsWithVos = {
    ...commonProps,
    data: {
        ...data,
        hasAccount: true
    },
    hasOgrn: true,
    isVosUser: true
};

describe('ContactsForm', () => {
    it("has ogrn block if AGENCY doesn't have vos account", () => {
        const wrapper = shallow(<ContactsForm {...propsWithoutVos} />);

        expect(wrapper.find('accountOGRN').length).toBe(1);
    });

    it("doesn't have ogrn block if AGENCY has vos account", () => {
        const wrapper = shallow(<ContactsForm {...propsWithVos} />);

        expect(wrapper.find('accountOGRN').length).toBe(0);
    });

    it("has ogrn block if AGENCY has vos account, but doesn't have ogrn", () => {
        const props = {
            ...propsWithVos,
            hasOgrn: false,
            isJuridical: true
        };

        const wrapper = shallow(<ContactsForm {...props} />);

        expect(wrapper.find('accountOGRN').length).toBe(1);
    });

    it("doesn't have ogrn block if user is OWNER", () => {
        const props = {
            ...propsWithVos,
            data: {
                ...propsWithVos.data,
                userType: 'OWNER'
            },
            hasOgrn: false,
            isJuridical: false
        };

        const wrapper = shallow(<ContactsForm {...props} />);

        expect(wrapper.find('accountOGRN').length).toBe(0);
    });
});
