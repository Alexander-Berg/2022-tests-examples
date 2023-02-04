import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import ClientMenu from '../index';
import styles from '../styles.module.css';

const CLIENT_ID = '233333';

const Component = ({ id = CLIENT_ID, userType = 'AGENT', page = 'clientTariffs', isJuridical = false }) => (
    <AppProviders>
        <ClientMenu
            errorCount={1337}
            clientId={id}
            page={page}
            userType={userType}
            isJuridical={isJuridical}
        />
    </AppProviders>
);

describe('ClientMenu', () => {
    it('Корректно строит ссылку на тарифы клиентов', async() => {
        await render(<Component isJuridical />, { viewport: { width: 220, height: 220 } });
        const href = await page.$eval(`.${styles.active}`, link => link.href);
        const expectedRegExp = new RegExp(`\/clients\/${CLIENT_ID}\/tariffs$`);

        expect(href).toMatch(expectedRegExp);
    });

    it('Корректно рисует меню у агента юр. лица', async() => {
        await render(<Component isJuridical />, { viewport: { width: 220, height: 350 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Корректно рисует меню без тарифов и алиасов у агента физ. лица', async() => {
        await render(<Component page="clientProfile" />, { viewport: { width: 220, height: 350 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Корректно рисует меню без алиасов у застройщика', async() => {
        await render(<Component page="clientProfile" isJuridical userType="DEVELOPER" />,
            { viewport: { width: 220, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Корректно рисует меню без тарифов и алиасов у РА', async() => {
        await render(
            <Component id="bil_12344" page="clientSitesCampaigns" isJuridical userType="AD_AGENCY" />,
            { viewport: { width: 220, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
