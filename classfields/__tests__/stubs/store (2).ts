import { IPaymentStore } from 'realty-core/view/react/deskpad/reducers/payment';
import { StageName } from 'realty-core/view/react/deskpad/reducers/payment/types';

export const store: { payment: IPaymentStore<'juridicalEGRNPaidReport'> } = {
    payment: {
        juridicalEGRNPaidReport: {
            popup: {
                isOpened: false,
                data: {},
            },
            stages: {
                init: {
                    isLoaded: false,
                },
                perform: {
                    isLoaded: false,
                },
                status: {
                    isLoaded: false,
                },
            },
            currentStageName: StageName.init,
            isLoading: false,
            hasError: false,
        },
    },
};
