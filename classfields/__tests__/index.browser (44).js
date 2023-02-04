/* eslint-disable jest/expect-expect */
import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

import { AppProvider } from 'view/lib/test-helpers';

import CallsList from '../index';

import { getState, defaultCall, RGIDS } from './mocks';

// 1220 - имитируем common-widget width
const Component = ({ state }) => (
    <AppProvider>
        <div style={{ maxWidth: 1220 }}>
            <CallsList
                {...state}
                onOpenOfferCallComplaintPopup={() => {}}
                openAudioPlayer={() => {}}
                closeAudioPlayer={() => {}}
            />
        </div>
    </AppProvider>
);

const SIZES = [
    { width: 918, height: 200 }, // минимальное разрешение для таблицы
    { width: 1341, height: 200 } // разрешение, на котором переключается "действие со звонком"
];

const renderAllSizes = async component => {
    // eslint-disable-next-line no-unused-vars
    for (const size of SIZES) {
        // eslint-disable-next-line no-await-in-loop
        await render(component, { viewport: size });

        // eslint-disable-next-line no-await-in-loop
        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

describe('CallsList', () => {
    describe('Столицы', () => {
        it('tuz unavailable, billed, in capital, after 01.04.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-04-28T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidLO
            } ], {
                isTuzAvailable: false,
                isTuzEnabled: true
            });

            allure.descriptionHtml(`
        <ol>
            <li>ТУЗ недоступен</li>
            <li>Звонок обилен</li>
            <li>Столица</li>
            <li>После 01.04.2020</li>
            <li>С записью звонка</li>
        </ol>
        `);

            await renderAllSizes(<Component state={state} />);
        });

        it('tuz unavailable, billed, in capital, after 01.04.2020, with record, bizdev', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-04-28T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidLO
            } ], {
                isTuzAvailable: false,
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, billed, in capital, after 01.04.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-04-28T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidLO
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, billed, in capital, after 01.04.2020, without record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-04-28T14:49:55.460Z',
                recordId: undefined,
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidLO
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, billed, in capital, before 01.04.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-03-28T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidLO
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, billed, in capital, before 01.04.2020, with record bizdev', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-03-28T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidLO
            } ], {
                isTuzEnabled: true,
                isBizdev: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, not billed, in capital, after 01.04.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-04-28T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: false,
                tuzTagRgid: RGIDS.rgidLO
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('without tuz, billed, in capital, after 01.04.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-04-28T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidLO
            } ], {
                isTuzEnabled: false
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('without tuz, not billed, in capital, after 01.04.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-04-28T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: false,
                tuzTagRgid: RGIDS.rgidLO
            } ], {
                isTuzEnabled: false
            });

            await renderAllSizes(<Component state={state} />);
        });
    });

    describe('Регионы без платности', () => {
        it('with tuz, billed, in regions without payment, with record, after 01.10.2020', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-10-02T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidIrkutskObl
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, billed, in regions without payment, with record, before 01.10.2020', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-11T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidIrkutskObl
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, billed, in regions without payment, without record, before 01.10.2020', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-11T14:49:55.460Z',
                recordId: undefined,
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidIrkutskObl
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, not billed, in regions without payment, with record, before 01.10.2020', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-11T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: false,
                tuzTagRgid: RGIDS.rgidIrkutskObl
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('without tuz, billed, in regions without payment, with record, before 01.10.2020', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-11T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidIrkutskObl
            } ], {
                isTuzEnabled: false
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, billed, in regions without payment, with record, before 01.10.2020, bizdev', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-11T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidIrkutskObl
            } ], {
                isTuzEnabled: true,
                isBizdev: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('without tuz, billed, in regions without payment, with record, before 01.10.2020, bizdev', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-11T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidIrkutskObl
            } ], {
                isTuzEnabled: false,
                isBizdev: true
            });

            await renderAllSizes(<Component state={state} />);
        });
    });

    describe('Первая волна платности в регионах', () => {
        it('with tuz, billed, after 12.07.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-12T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidKK
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('without tuz, billed, after 12.07.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-12T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidKK
            } ], {
                isTuzEnabled: false
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('without tuz, not billed, after 12.07.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-12T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: false,
                tuzTagRgid: RGIDS.rgidKK
            } ], {
                isTuzEnabled: false
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, billed, before 12.07.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-11T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidKK
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('with tuz, not billed, before 12.07.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-11T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: false,
                tuzTagRgid: RGIDS.rgidKK
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('without tuz, billed, before 12.07.2020, with record', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-07-11T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidKK
            } ], {
                isTuzEnabled: false
            });

            await renderAllSizes(<Component state={state} />);
        });
    });

    describe('Вторая волна платности в регионах', () => {
        it('туз, обилен, после 15.03.2021, с записью звонка', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2021-04-16T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidNizhnyNovgorodObl
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('нет туз, обилен, после 15.03.2021, с записью звонка', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2021-04-16T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidNizhnyNovgorodObl
            } ], {
                isTuzEnabled: false
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('нет туз, не обилен, после 15.03.2021, с записью звонка', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2021-04-16T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: false,
                tuzTagRgid: RGIDS.rgidNizhnyNovgorodObl
            } ], {
                isTuzEnabled: false
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('туз, обилен, до 15.03.2021, с записью звонка', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-03-13T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidNizhnyNovgorodObl
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('туз, не обилен, до 15.03.2021, с записью звонка', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-03-13T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: false,
                tuzTagRgid: RGIDS.rgidNizhnyNovgorodObl
            } ], {
                isTuzEnabled: true
            });

            await renderAllSizes(<Component state={state} />);
        });

        it('нет туз, обилен, до 15.03.2021, с записью звонка', async() => {
            const state = getState([ {
                ...defaultCall,
                timestamp: '2020-03-13T14:49:55.460Z',
                recordId: 'TEST',
                payedTuzCall: true,
                tuzTagRgid: RGIDS.rgidNizhnyNovgorodObl
            } ], {
                isTuzEnabled: false
            });

            await renderAllSizes(<Component state={state} />);
        });
    });

    it('Звонок из профиля', async() => {
        const state = getState([ {
            ...defaultCall,
            callType: 'profile',
            timestamp: '2020-07-11T14:49:55.460Z',
            recordId: 'TEST',
            payedTuzCall: true,
            tuzTagRgid: RGIDS.rgidMO
        } ], {
            isTuzEnabled: true,
            isBizdev: false
        });

        await renderAllSizes(<Component state={state} />);
    });
});
