import { agentProfiles2 } from './agentProfiles';

export const redirectPhonesGateStub = {
    phones: [{ wholePhoneNumber: '+79992134916' }, { wholePhoneNumber: '+79992134916' }],
};

export const geoSuggestGateStub = [{ rgid: 255535, label: 'Новосибирская область' }];

export const secondProfilesPageGateStub = {
    page: { current: 2, total: 3 },
    profiles: agentProfiles2,
};
