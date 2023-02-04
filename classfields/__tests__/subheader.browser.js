import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { getAllowedLKSections } from 'realty-core/app/lib/allowed-lk-sections';

import { SubheaderComponent } from '../';

describe('subheader', () => {
    it('should not crash if not given any props', async() => {
        await render(
            <SubheaderComponent />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for userType: AGENCY || min width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'AGENCY',
                isJuridical: true,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for userType: AD_AGENCY || min width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'AD_AGENCY',
                isJuridical: true,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for userType: AD_AGENCY with selected client || min width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'AD_AGENCY',
                isJuridical: true,
                isAuth: true,
                hasSelectedAgencyClient: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for userType: AGENT || min width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'AGENT',
                isJuridical: true,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for userType: NATURAL_PERSON || min width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'NATURAL_PERSON',
                isJuridical: false,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for non-vos userType: NATURAL_PERSON || min width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: false,
                userType: 'NATURAL_PERSON',
                isJuridical: false,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for moderator || min width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'NATURAL_PERSON',
                isJuridical: false,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: true,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for userType: AGENCY || max width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'AGENCY',
                isJuridical: true,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1220, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for userType: AD_AGENCY || max width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'AD_AGENCY',
                isJuridical: true,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1220, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for userType: AD_AGENCY with selected client || max width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'AD_AGENCY',
                isJuridical: true,
                isAuth: true,
                hasSelectedAgencyClient: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1220, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for userType: AGENT || max width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'AGENT',
                isJuridical: true,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1220, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for userType: NATURAL_PERSON || max width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'NATURAL_PERSON',
                isJuridical: false,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1220, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for non-vos userType: NATURAL_PERSON || max width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: false,
                userType: 'NATURAL_PERSON',
                isJuridical: false,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: false,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1220, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render subheader for moderator || max width', async() => {
        const mockProps = {
            tabConfigs: getAllowedLKSections({
                isVosUser: true,
                userType: 'NATURAL_PERSON',
                isJuridical: false,
                isAuth: true
            }),
            vosUserLogin: '123123123',
            isModerator: true,
            currentTab: 'favorites'
        };

        await render(
            <SubheaderComponent
                {...mockProps}
            />,
            { viewport: { width: 1220, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
