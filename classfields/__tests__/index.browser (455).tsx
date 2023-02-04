import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';

import PhoneModal from '../';

import {
    storeWithOnePhoneDeveloperOffer,
    storeWithThreePhoneDeveloperOffer,
    storeWithOnePhoneAgencyOffer,
    storeWithOnePhoneAuthorOffer,
    storeWithOnePhoneAuthorWithoutProtectOffer,
    storeWithOnePhoneAuthorNotTrustOffer,
    storeWithOnePhoneAuthorToAuthorOffer,
    storeWithThreePhoneAuthorOffer,
    storeWithFourPhoneAuthorWithoutProtectOffer,
    storeWithOriginalSellerDeveloperOnePhoneOffer,
    storeWithOriginalSellerAgencyThreePhoneOffer,
    storeWithOriginalSellerOwnerOnePhoneOffer,
    storeWithOriginalSellerAgentTwoPhoneOffer,
} from './mocks';

function Component(props: Partial<IAppProviderProps>) {
    return (
        <AppProvider
            {...props}
            fakeTimers={{
                now: new Date('2021-08-03T14:00:00.111Z').getTime(),
            }}
        >
            <PhoneModal />
        </AppProvider>
    );
}

describe('PhoneModal', () => {
    it('без originalSeller, автор застройщик с одним телефоном', async () => {
        await render(<Component initialState={storeWithOnePhoneDeveloperOffer} />, {
            viewport: { width: 1000, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('без originalSeller, автор застройщик с тремя телефонами', async () => {
        await render(<Component initialState={storeWithThreePhoneDeveloperOffer} />, {
            viewport: { width: 1000, height: 700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('без originalSeller, автор агентство с одним телефоном', async () => {
        await render(<Component initialState={storeWithOnePhoneAgencyOffer} context={{ link: () => '123' }} />, {
            viewport: { width: 1000, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('без originalSeller, автор собственник с одним телефоном', async () => {
        await render(<Component initialState={storeWithOnePhoneAuthorOffer} />, {
            viewport: { width: 1000, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('без originalSeller, автор собственник с одним телефоном (без подменника)', async () => {
        await render(<Component initialState={storeWithOnePhoneAuthorWithoutProtectOffer} />, {
            viewport: { width: 1000, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('без originalSeller, автор собственник с одним телефоном (профиль не привязан)', async () => {
        await render(<Component initialState={storeWithOnePhoneAuthorNotTrustOffer} />, {
            viewport: { width: 1000, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('без originalSeller, автор агент с одним телефоном (показ самому себе)', async () => {
        await render(
            <Component initialState={storeWithOnePhoneAuthorToAuthorOffer} context={{ link: () => '123' }} />,
            { viewport: { width: 1000, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('без originalSeller, автор частный агент с тремя телефонами', async () => {
        await render(<Component initialState={storeWithThreePhoneAuthorOffer} />, {
            viewport: { width: 1000, height: 700 },
        });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('без originalSeller, автор собственник с четырьмя телефонами (без подменников)', async () => {
        await render(<Component initialState={storeWithFourPhoneAuthorWithoutProtectOffer} />, {
            viewport: { width: 1000, height: 900 },
        });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('c originalSeller, автор застройщик с одним телефоном', async () => {
        await render(<Component initialState={storeWithOriginalSellerDeveloperOnePhoneOffer} />, {
            viewport: { width: 1000, height: 700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('c originalSeller, автор агентство с тремя телефонами', async () => {
        await render(<Component initialState={storeWithOriginalSellerAgencyThreePhoneOffer} />, {
            viewport: { width: 1000, height: 800 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('c originalSeller, автор собственник с одним телефоном', async () => {
        await render(<Component initialState={storeWithOriginalSellerOwnerOnePhoneOffer} />, {
            viewport: { width: 1000, height: 700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('c originalSeller, автор агент с двумя телефонами', async () => {
        await render(<Component initialState={storeWithOriginalSellerAgentTwoPhoneOffer} />, {
            viewport: { width: 1000, height: 800 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
