import React from 'react';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';

import { COMPLAINT_STATUS, COMPLAINT_MANUAL_STATUS } from 'types/callComplaintInfo';

import { AppProvider } from 'view/lib/test-helpers';
import 'view/deskpad/common.css';

import { DashboardWidgetAudioPlayerContainer } from '../container';

const SCREENS = [
    [1000, 100],
    [1400, 100],
];

const getState = (stateOverrides = {}) => {
    return merge(
        {
            audioplayer: {
                isOpened: true,
                isLoading: false,
                complaintInfo: undefined,
                call: {
                    recordId: '123',
                    offerId: '123',
                    address: 'Москва, Конюшковская улица 28',
                    callId: '123',
                    payedTuzCall: true,
                    timestamp: '2020-11-01 10:11:00',
                    tuzTagRgid: 417899,
                },
                audioUrl: '',
            },
            geo: {
                rgidSpb: 417899,
            },
            tuz: {
                tuzType: {
                    extended: {},
                },
            },
        },
        stateOverrides
    );
};

const renderOnScreens = async (component: React.ReactElement) => {
    for (const [WIDTH, HEIGHT] of SCREENS) {
        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

const Component: React.FunctionComponent<Partial<IAppProviderProps>> = ({ initialState }) => (
    <AppProvider initialState={initialState}>
        <DashboardWidgetAudioPlayerContainer />
    </AppProvider>
);

describe('DashboardWidgetAudioPlayerContainer', () => {
    it('Длинный адрес', async () => {
        const store = getState({
            audioplayer: {
                call: {
                    offerId: '123',
                    address: 'Москва, Конюшковская улица имени красивых молдавских партизан, 28',
                    callId: '123',
                    payedTuzCall: undefined,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Объявление не определено', async () => {
        const store = getState({
            audioplayer: {
                call: {
                    offerId: null,
                    address: null,
                    callId: '123',
                    payedTuzCall: true,
                },
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.IsAbleToComplain,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Звонок из профиля', async () => {
        const store = getState({
            audioplayer: {
                call: {
                    callType: 'profile',
                    offerId: null,
                    address: null,
                    callId: '123',
                    payedTuzCall: false,
                },
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.IsAbleToComplain,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('В процессе загрузки', async () => {
        const store = getState({
            audioplayer: {
                isLoading: true,
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Платный звонок, регион без обязательной платности', async () => {
        const store = getState({
            audioplayer: {
                call: {
                    tuzTagRgid: 212,
                },
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.IsAbleToComplain,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Не обиленный звонок', async () => {
        const store = getState({
            audioplayer: {
                call: {
                    payedTuzCall: false,
                },
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.IsAbleToComplain,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Платный звонок, статус жалобы - "OnModeration"', async () => {
        const store = getState({
            audioplayer: {
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.OnModeration,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Платный звонок, статус жалобы - "IsAbleToComplain"', async () => {
        const store = getState({
            audioplayer: {
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.IsAbleToComplain,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Платный звонок, статус жалобы - "AlreadyModerated, pass"', async () => {
        const store = getState({
            audioplayer: {
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.AlreadyModerated,
                    manual: COMPLAINT_MANUAL_STATUS.pass,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Платный звонок, статус жалобы - "AlreadyModerated, fail"', async () => {
        const store = getState({
            audioplayer: {
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.AlreadyModerated,
                    manual: COMPLAINT_MANUAL_STATUS.fail,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Платный звонок, статус жалобы - "NotBilledCall"', async () => {
        const store = getState({
            audioplayer: {
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.NotBilledCall,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Платный звонок, статус жалобы - "WithoutAnswer"', async () => {
        const store = getState({
            audioplayer: {
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.WithoutAnswer,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });

    it('Платный звонок, статус жалобы - "TooOld"', async () => {
        const store = getState({
            audioplayer: {
                complaintInfo: {
                    complaintStatus: COMPLAINT_STATUS.TooOld,
                },
            },
        });
        const component = <Component initialState={store} />;

        await renderOnScreens(component);
    });
});
