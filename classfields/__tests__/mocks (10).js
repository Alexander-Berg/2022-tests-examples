import merge from 'lodash/merge';
import cloneDeep from 'lodash/cloneDeep';

import { contactsStateProducerFactory } from '../../SettingsContacts/__tests__/mocks';
import { requisitesStateProducerFactory } from '../../SettingsRequisitesForm/__tests__/stubs/store';
import { profileStateProducerFactory } from '../../SettingsProfile/__tests__/mocks';

const getState = (stateOverrides = {}) => {
    const { vosUserData } = stateOverrides;
    const requisitesProducer = requisitesStateProducerFactory({}, vosUserData);
    const contactsProducer = contactsStateProducerFactory(vosUserData);
    const profileProducer = profileStateProducerFactory(vosUserData);

    return merge({
        config: {
            mosruRedirectLinks: {
                url: ''
            }
        },
        billingRequisites: { byId: {}, ids: [] },
        vosUserData: {
            userType: 'OWNER',
            phones: [],
            trustedUserInfo: {
                mosRuTrustedStatus: 'NOT_PROCESSED',
                mosRuAvailable: false
            }
        },
        user: { isJuridical: false },
        settings: {
            requisites: requisitesProducer(),
            contacts: contactsProducer(),
            profile: profileProducer(),
            network: {
                contactsAndProfileSaveStatus: 'success',
                sendingConfirmationCodeStatus: 'success',
                uploadingTrademarkStatus: 'success'
            }
        }
    }, stateOverrides);
};

const naturalState = getState();

const naturalNoVosState = getState({ vosUserData: null });

const juridicalState = getState({
    user: { isJuridical: true },
    vosUserData: { userType: 'AGENCY' }
});

const adAgencyState = getState({
    user: { isJuridical: true },
    vosUserData: { userType: 'AD_AGENCY' }
});

export default {
    natural: naturalState,
    juridical: juridicalState,
    juridicalWithoutBillingRequisites: { ...cloneDeep(juridicalState), billingRequisites: {
        byId: {},
        ids: [],
        isLoadingError: true
    } },
    adAgency: adAgencyState,
    noVos: naturalNoVosState,
    adAgencyWithoutBillingRequisites: { ...cloneDeep(adAgencyState), billingRequisites: {
        byId: {},
        ids: [],
        isLoadingError: true
    } }
};

