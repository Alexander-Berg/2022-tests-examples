import merge from 'lodash/merge';

const initialState = {
    user: {
        isJuridical: true
    },
    popups: {
        offerCallComplaint: {
            visible: true,
            complaintFetchingStatus: 'loading',
            reportComplaintStatus: 'initial',
            complaintInfo: {},
            complaintText: ''
        }
    },
    billing: {
        client: { id: 1337 },
        campaign: { id: 1338 }
    }
};

const getStore = (popupOverrides = {}, storeOverrides = {}) => {
    const popups = {
        popups: {
            offerCallComplaint: merge(initialState.popups.offerCallComplaint, popupOverrides)
        }
    };

    return merge({}, initialState, storeOverrides, popups);
};

export default {
    fetching: getStore(),
    error: getStore({ complaintFetchingStatus: 'error' }),
    tooOld: getStore({ complaintFetchingStatus: 'success', complaintInfo: { complaintStatus: 'TooOld' } }),
    notBilledCall: getStore(
        { complaintFetchingStatus: 'success', complaintInfo: { complaintStatus: 'NotBilledCall' } }
    ),
    onModeration: getStore({ complaintFetchingStatus: 'success', complaintInfo: { complaintStatus: 'OnModeration' } }),
    withoutAnswer: getStore(
        { complaintFetchingStatus: 'success', complaintInfo: { complaintStatus: 'WithoutAnswer' } }
    ),
    alreadyModeratedPass: getStore(
        { complaintFetchingStatus: 'success', complaintInfo: { complaintStatus: 'AlreadyModerated', manual: 'Pass' } }
    ),
    alreadyModeratedFail: getStore(
        { complaintFetchingStatus: 'success', complaintInfo: { complaintStatus: 'AlreadyModerated', manual: 'Fail' } }
    ),
    isAbleToComplainClear: getStore({
        complaintFetchingStatus: 'success',
        complaintInfo: { complaintStatus: 'IsAbleToComplain' }
    }),
    isAbleToComplainFilled: getStore({
        complaintFetchingStatus: 'success',
        complaintText: 'Какой-то странный у вас сервис',
        complaintInfo: { complaintStatus: 'IsAbleToComplain' }
    }),
    isAbleToComplainLoading: getStore({
        complaintFetchingStatus: 'success',
        reportComplaintStatus: 'loading',
        complaintText: 'Какой-то странный у вас сервис',
        complaintInfo: { complaintStatus: 'IsAbleToComplain' }
    }),
    isAbleToComplainSuccess: getStore({
        complaintFetchingStatus: 'success',
        reportComplaintStatus: 'success',
        complaintText: 'Какой-то странный у вас сервис',
        complaintInfo: { complaintStatus: 'IsAbleToComplain' }
    }),
    isAbleToComplainFailed: getStore({
        complaintFetchingStatus: 'success',
        reportComplaintStatus: 'error',
        complaintText: 'Какой-то странный у вас сервис',
        complaintInfo: { complaintStatus: 'IsAbleToComplain' }
    })
};
