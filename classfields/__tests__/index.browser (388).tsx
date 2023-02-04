import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, rejectPromise } from 'realty-core/view/react/libs/test-helpers';
import { IOfferContact } from 'realty-core/types/phones';
import { IOfferCard } from 'realty-core/types/offerCard';

import { OfferCardOwnerPhone } from '../index';

import styles from './styles.module.css';

const Component: React.FC<{
    Gate?: Record<string, unknown>;
    offerId: string;
    offerPhones?: Record<number | string, IOfferContact[]>;
    loadPhonesOnMountAllowed?: boolean;
}> = ({ Gate, offerId, offerPhones = {}, loadPhonesOnMountAllowed }) => (
    <AppProvider initialState={{ offerPhones, user: { isAuth: true, uid: '777' }, offerCard: {} }} Gate={Gate}>
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column' }}>
            <OfferCardOwnerPhone
                offer={
                    ({
                        offerId,
                    } as unknown) as IOfferCard
                }
                page="blank"
                pageType="blank"
                placement="somewhere"
                eventPlace="card_top"
                className={styles.root}
                loadPhonesOnMountAllowed={loadPhonesOnMountAllowed}
            />
        </div>
    </AppProvider>
);

describe('OfferCardOwnerPhone', () => {
    it('Нек рисуется, если нет номера', async () => {
        await render(<Component offerId="1" />, {
            viewport: { width: 400, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с одним номером', async () => {
        const offerPhones = {
            123: [
                {
                    shouldShowRedirectIndicator: false,
                    withBilling: false,
                    phones: [{ phoneNumber: '+79991150830' }],
                },
            ],
        };
        await render(<Component offerId="123" offerPhones={offerPhones} />, {
            viewport: { width: 400, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с двумя номерами', async () => {
        const offerPhones = {
            123: [
                {
                    shouldShowRedirectIndicator: false,
                    withBilling: false,
                    phones: [{ phoneNumber: '+79991150830' }, { phoneNumber: '+79991150831' }],
                },
            ],
        };
        await render(<Component offerId="123" offerPhones={offerPhones} />, {
            viewport: { width: 400, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с одним защищённым номером', async () => {
        const offerPhones = {
            123: [
                {
                    shouldShowRedirectIndicator: true,
                    isRedirectPhones: true,
                    withBilling: false,
                    phones: [{ phoneNumber: '+79991150830' }],
                },
            ],
        };
        await render(<Component offerId="123" offerPhones={offerPhones} />, {
            viewport: { width: 400, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с двумя защищёнными номерами', async () => {
        const offerPhones = {
            123: [
                {
                    shouldShowRedirectIndicator: true,
                    isRedirectPhones: true,
                    withBilling: false,
                    phones: [{ phoneNumber: '+79991150830' }, { phoneNumber: '+79991150831' }],
                },
            ],
        };
        await render(<Component offerId="123" offerPhones={offerPhones} />, {
            viewport: { width: 400, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии ошибки', async () => {
        const Gate = {
            get: () => rejectPromise(),
        };

        await render(<Component offerId="1" Gate={Gate} loadPhonesOnMountAllowed />, {
            viewport: { width: 400, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
