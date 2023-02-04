import merge from 'lodash/merge';

export const RGIDS = {
    rgidSpb: 417899,
    rgidMsk: 587795,
    rgidMO: 741964,
    rgidLO: 741965,

    rgidKK: 353118,
    rgidVoronezhObl: 475531,
    rgidRostovObl: 211571,

    rgidSverdObl: 326698,
    rgidTatarstan: 426660,
    rgidNizhnyNovgorodObl: 426764,

    rgidIrkutskObl: 281170
};

const initialState = {
    mandatoryPaymentRgidMap: {
        [RGIDS.rgidSpb]: true,
        [RGIDS.rgidMsk]: true,
        [RGIDS.rgidMO]: true,
        [RGIDS.rgidLO]: true
    },
    mandatoryJuridicalPaymentCallRgidMapRegionsWave1: {
        [RGIDS.rgidKK]: true,
        [RGIDS.rgidVoronezhObl]: true,
        [RGIDS.rgidRostovObl]: true
    },
    mandatoryJuridicalPaymentCallRgidMapRegionsWave2: {
        [RGIDS.rgidSverdObl]: true,
        [RGIDS.rgidTatarstan]: true,
        [RGIDS.rgidNizhnyNovgorodObl]: true
    },
    isTuzAvailable: true,
    isTuzEnabled: true,
    isBizdev: false,
    loading: false,
    error: false
};

export const getState = (calls = [], stateOverrides = {}) => {
    const callsList = [
        {
            calls: [].concat(calls),
            pager: {
                page: 0,
                pageSize: 30,
                totalItems: 2,
                totalPages: 1
            }
        }
    ];

    return merge({}, initialState, { callsList }, stateOverrides);
};

export const defaultCall = {
    timestamp: '2020-04-28T14:49:55.460Z',
    callType: 'target',
    sourcePhone: '+74951173112',
    destinationPhone: '+79213122439',
    duration: 20,
    id: 'TEST',
    recordId: 'TEST',
    payedTuzCall: true,
    tuzTagRgid: RGIDS.rgidLO,
    discountedPrice: 1000,
    offer: {
        id: '410379085507542084',
        offerType: 'SELL',
        offerCategory: 'APARTMENT',
        address: 'Кантемировская, 24'
    }
};
