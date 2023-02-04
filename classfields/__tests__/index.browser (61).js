import React from 'react';
import { connect } from 'react-redux';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { WithScrollContextProvider } from 'realty-core/view/react/common/enhancers/withScrollContext';

import { AppProvider } from 'view/lib/test-helpers';

import { SettingsContactsAndProfile } from '../index';

import { isProfileAvailableSelector } from '../../Settings/container';

import mocks from './mocks';

const SettingsContactsAndProfileContainer = connect((state, ownProps) => ({
    isProfileAvailable: ownProps.isProfileAvailable !== undefined ?
        ownProps.isProfileAvailable :
        isProfileAvailableSelector(state)
}))(SettingsContactsAndProfile);

const Component = ({ store, isProfileAvailable, scrollToErrors }) => (
    <AppProvider initialState={store}>
        <WithScrollContextProvider offest={0}>
            <SettingsContactsAndProfileContainer
                isProfileAvailable={isProfileAvailable}
                scrollToErrors={scrollToErrors}
            />
        </WithScrollContextProvider>
    </AppProvider>
);

const userTypeButtonSelectorFactory = n => `#settings_contacts_user_type .Radio:nth-child(${n})`;
const addressInputSelector = '#settings_profile_address .TextInput__control';
const phoneTumblerSelectorFactory = n => `#settings_contacts_phones_${n} .Tumbler`;
const ogrnInputSelector = '#settings_contacts_ogrn .TextInput__control';
const submitButtonSelector = '#settings_contacts_and_profile_submit_form .Button';
const timeFromInputSelector = '#working_hours_range_from';
const timeToInputSelector = '#working_hours_range_to';
const isEnabledTumblerSelector = '#settings_profile_header .Tumbler__button';
const workingDayTagFactory = n => `#settings_profile_working_days .CheckboxGroup .Checkbox:nth-child(${n})`;
const nameInputSelector = '#settings_contacts_name .TextInput__control';

describe('SettingsContactsAndProfile', () => {
    describe('view', () => {
        it('agent without profile', async() => {
            await render(
                <Component store={mocks.agent} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agent juridical without profile', async() => {
            await render(
                <Component store={mocks.agentJuridical} isProfileAvailable={false} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agent juridical with profile', async() => {
            await render(
                <Component store={mocks.agentJuridical} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agency without profile', async() => {
            await render(
                <Component store={mocks.agency} isProfileAvailable={false} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agency with profile', async() => {
            await render(
                <Component store={mocks.agency} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('logic', () => {
        it('owner want to be agent with profile', async() => {
            await render(
                <Component store={mocks.owner} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(userTypeButtonSelectorFactory(2));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(ogrnInputSelector, '1177746415857');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('owner want to be agency with profile', async() => {
            await render(
                <Component store={mocks.owner} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(userTypeButtonSelectorFactory(3));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('show profile when agent filled ogrn', async() => {
            await render(
                <Component store={mocks.agent} />,
                { viewport: { width: 1400, height: 1600 } }
            );
            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(ogrnInputSelector, '1177746415857');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agent want to be agency with profile', async() => {
            await render(
                <Component store={mocks.agent} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(userTypeButtonSelectorFactory(2));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('display messages with invalid fields on contacts and profile when submitting', async() => {
            await render(
                <Component store={mocks.agency} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            await page.type(nameInputSelector, 'Гы');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(submitButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(submitButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('dont submitting natural without checked phones', async() => {
            await render(
                <Component store={mocks.ownerWithoutCheckedPhones} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(phoneTumblerSelectorFactory(0));

            await page.click(phoneTumblerSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(submitButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(phoneTumblerSelectorFactory(0));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('can save working days and working time only together, trying days', async() => {
            await render(
                <Component store={mocks.filledAgency} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(workingDayTagFactory(1));
            await page.click(workingDayTagFactory(5));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(submitButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(timeFromInputSelector, '10:00');
            await page.type(timeToInputSelector, '15:00');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(workingDayTagFactory(1));
            await page.click(workingDayTagFactory(5));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(submitButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('profile address for agent and agency sync', async() => {
            await render(
                <Component store={mocks.filledAgent} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(addressInputSelector, 'test');
            await page.$eval(addressInputSelector, e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(userTypeButtonSelectorFactory(2));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('submit not disabled when has moderation messages', async() => {
            await render(
                <Component store={mocks.agencyWithModerationErrors} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(userTypeButtonSelectorFactory(2));
            await page.click(userTypeButtonSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('disable all backend duplicate errors, when profile was disabled', async() => {
            await render(
                <Component store={mocks.agencyWithBackendDuplicateErrors} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('disable all moderation errors when profile was disable', async() => {
            await render(
                <Component store={mocks.agencyWithModerationErrors} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('disable all moderation errors when agency wants to be developer', async() => {
            await render(
                <Component store={mocks.agencyWithModerationErrors} />,
                { viewport: { width: 1400, height: 1600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(userTypeButtonSelectorFactory(2));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(userTypeButtonSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('scrolling to errors', async() => {
            await render(
                <Component store={mocks.agencyWithModerationErrors} scrollToErrors />,
                { viewport: { width: 1400, height: 500 } }
            );

            await page.evaluate(selector => {
                const element = document.querySelector(selector);

                element.scrollIntoView();
            }, submitButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(submitButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
