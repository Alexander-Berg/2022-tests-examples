jest.mock('auto-core/react/dataDomain/credit/mappers/addressEntityFromSuggest', () => {
    return (address: any) => address;
});

import dadataAddressMock from 'auto-core/react/dataDomain/credit/mocks/dadataAddress.mock';
import dadataAddress2Mock from 'auto-core/react/dataDomain/credit/mocks/dadataAddress2.mock';

import type { PersonProfile } from 'auto-core/types/TPersonProfile';
import type { FlagsInfo } from 'auto-core/types/TCreditBroker';
import CreditFormFieldType from 'auto-core/types/TCreditFormField';

import mapAddressSection from './mapAddressSection';

it('с местом проживания по месту регистрации', () => {
    const personProfile = {} as PersonProfile;
    const flags = {} as FlagsInfo;

    const values = {
        [CreditFormFieldType.REGISTRATION_ADDRESS]: dadataAddressMock,
        [CreditFormFieldType.ADDRESSES_ARE_SAME]: true,
    };

    mapAddressSection({ values, personProfile, errors: {}, flags });

    expect(personProfile.registration_address?.address_entity).toEqual(dadataAddressMock);
    expect(personProfile.residence_address?.address_entity).toEqual(personProfile.registration_address?.address_entity);
    expect(flags.is_the_same_address).toEqual(true);
});

it('с разными местом проживания и местом регистрации', () => {
    const personProfile = {} as PersonProfile;
    const flags = {} as FlagsInfo;

    const values = {
        [CreditFormFieldType.REGISTRATION_ADDRESS]: dadataAddressMock,
        [CreditFormFieldType.RESIDENCE_ADDRESS]: dadataAddress2Mock,
        [CreditFormFieldType.ADDRESSES_ARE_SAME]: false,
    };

    mapAddressSection({ values, personProfile, errors: {}, flags });

    expect(personProfile.registration_address?.address_entity).toEqual(dadataAddressMock);
    expect(personProfile.residence_address?.address_entity).toEqual(dadataAddress2Mock);
    expect(flags).not.toHaveProperty('is_the_same_address');
});

it('с заполненной датой регистрации', () => {
    const personProfile = {} as PersonProfile;
    const flags = {} as FlagsInfo;

    const values = {
        [CreditFormFieldType.REGISTRATION_ADDRESS]: dadataAddressMock,
        [CreditFormFieldType.REGISTRATION_DATE]: '24.10.2020',
        [CreditFormFieldType.ADDRESSES_ARE_SAME]: true,
    };

    mapAddressSection({ values, personProfile, errors: {}, flags });

    expect(personProfile.registration_address?.address_entity).toEqual(dadataAddressMock);
    expect(personProfile.registration_address?.registration_date).toEqual('2020-10-24T00:00:00.000Z');
    expect(personProfile.residence_address?.address_entity).toEqual(personProfile.registration_address?.address_entity);
    expect(flags.is_the_same_address).toEqual(true);
});

it('сохранит старую дату регистрации, если сейчас её нет в форме', () => {
    const personProfile = {
        registration_address: {
            registration_date: '1234',
        },
    } as PersonProfile;
    const flags = {} as FlagsInfo;

    const values = {
        [CreditFormFieldType.REGISTRATION_ADDRESS]: dadataAddressMock,
        [CreditFormFieldType.ADDRESSES_ARE_SAME]: true,
    };

    mapAddressSection({ values, personProfile, errors: {}, flags });

    expect(personProfile.registration_address?.address_entity).toEqual(dadataAddressMock);
    expect(personProfile.registration_address?.registration_date).toEqual('1234');
});
