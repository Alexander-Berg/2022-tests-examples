import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { WithScrollContextProvider } from 'realty-core/view/react/common/enhancers/withScrollContext';

import { AppProvider } from 'view/lib/test-helpers';

import SettingsProfileContainer from '../container';

import mocks from './mocks';

const Component = ({ store }) => (
    <AppProvider initialState={store}>
        <WithScrollContextProvider offest={0}>
            <SettingsProfileContainer className="" />
        </WithScrollContextProvider>
    </AppProvider>
);

const addressInputSelector = '#settings_profile_address .TextInput__control';
const timeFromInputSelector = '#working_hours_range_from';
const foundationDateInputSelector = '#settings_profile_foundation_date .TextInput__control';
const isEnabledTumblerSelector = '#settings_profile_header .Tumbler__button';
const logoPreviewSelector = '#settings_profile_header_preview';
const logoPreviewDeleteButtonSelector = '#settings_profile_header_preview_delete';
const descriptionTextAreaSelector = '#settings_profile_description .TextArea';
const workingDayTagFactory = n => `#settings_profile_working_days .CheckboxGroup .Checkbox:nth-child(${n})`;

describe('SettingsProfile', () => {
    describe('view', () => {
        it('default agency 1000', async() => {
            await render(
                <Component store={mocks.defaultAgency} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('default agent 1400', async() => {
            await render(
                <Component store={mocks.defaultAgent} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('published 1000', async() => {
            await render(
                <Component store={mocks.published} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('rejected moderation 1000', async() => {
            await render(
                <Component store={mocks.rejectedModeration} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('disabled profile 1000', async() => {
            await render(
                <Component store={mocks.disabledProfile} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('on moderation with published profile', async() => {
            await render(
                <Component store={mocks.onModerationWithPublishedProfile} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('rejected with published 1000', async() => {
            await render(
                <Component store={mocks.rejectedModerationWithPublishedProfile} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('will be disabled 1000', async() => {
            await render(
                <Component store={mocks.willBeDisabled} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('will be published 1000', async() => {
            await render(
                <Component store={mocks.willBePublished} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('will be republished 1000', async() => {
            await render(
                <Component store={mocks.willBeRepublished} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('errors', () => {
        it('not filled agency 1000', async() => {
            await render(
                <Component store={mocks.defaultNotFilledAgencyErrors} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('not filled agency 1400', async() => {
            await render(
                <Component store={mocks.defaultNotFilledAgencyErrors} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('not filled agent 1000', async() => {
            await render(
                <Component store={mocks.defaultNotFilledAgentErrors} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('not filled agent 1400', async() => {
            await render(
                <Component store={mocks.defaultNotFilledAgentErrors} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('invalid input 1000', async() => {
            await render(
                <Component store={mocks.defaultAgency} />,
                { viewport: { width: 1000, height: 900 } }
            );

            await page.type(addressInputSelector, 'Иркутск');
            await page.type(timeFromInputSelector, '1');
            await page.type(foundationDateInputSelector, '33');
            // клик для того, чтобы скрыть дейтпикер
            await page.mouse.click(0, 0);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('invalid input 1400', async() => {
            await render(
                <Component store={mocks.defaultAgency} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.type(addressInputSelector, 'Иркутск');
            await page.type(timeFromInputSelector, '1');
            await page.type(foundationDateInputSelector, '33');
            // клик для того, чтобы скрыть дейтпикер
            await page.mouse.click(0, 0);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('moderation set one for agency 1000', async() => {
            await render(
                <Component store={mocks.moderationErrors.setOneAgency} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('moderation set one for agency 1400', async() => {
            await render(
                <Component store={mocks.moderationErrors.setOneAgency} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('moderation set one for agent 1000', async() => {
            await render(
                <Component store={mocks.moderationErrors.setOneAgent} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('moderation set one for agent 1400', async() => {
            await render(
                <Component store={mocks.moderationErrors.setOneAgent} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('moderation set two agency 1000', async() => {
            await render(
                <Component store={mocks.moderationErrors.setTwoAgency} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('moderation set two agency 1400', async() => {
            await render(
                <Component store={mocks.moderationErrors.setTwoAgency} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('moderation set two agent 1000', async() => {
            await render(
                <Component store={mocks.moderationErrors.setTwoAgent} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('moderation set two agent 1400', async() => {
            await render(
                <Component store={mocks.moderationErrors.setTwoAgent} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('moderation set three 1000', async() => {
            await render(
                <Component store={mocks.moderationErrors.setThree} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('moderation set three 1400', async() => {
            await render(
                <Component store={mocks.moderationErrors.setThree} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('failed to upload agency logo', async() => {
            await render(
                <Component store={mocks.uploadingAgencyLogoFailed} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('failed to upload agent photo', async() => {
            await render(
                <Component store={mocks.uploadingAgentPhotoFailed} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('too large agent photo', async() => {
            await render(
                <Component store={mocks.tooLargeAgentPhoto} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('too large agency logo', async() => {
            await render(
                <Component store={mocks.tooLargeAgencyLogo} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(logoPreviewDeleteButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('logic', () => {
        it('tumbler switching', async() => {
            await render(
                <Component store={mocks.defaultAgency} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('tumbler switching with published profile', async() => {
            await render(
                <Component store={mocks.published} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('should hide validation messages on not filled inputs after disabling profile', async() => {
            await render(
                <Component store={mocks.defaultNotFilledAgencyErrors} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('working days selecting', async() => {
            await render(
                <Component store={mocks.defaultAgency} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(workingDayTagFactory(1));
            await page.click(workingDayTagFactory(3));
            await page.click(workingDayTagFactory(6));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(workingDayTagFactory(3));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('logo deleting', async() => {
            await render(
                <Component store={mocks.defaultFilled} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.hover(logoPreviewSelector);
            await page.click(logoPreviewDeleteButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('unknown error message dont disappear', async() => {
            await render(
                <Component store={mocks.moderationErrors.setThree} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('message on input with moderation error not disappear after blur event', async() => {
            await render(
                <Component store={mocks.moderationErrors.setTwoAgency} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.focus(descriptionTextAreaSelector);
            await page.$eval(descriptionTextAreaSelector, e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('trying to fix moderation errors and then disable profile', async() => {
            await render(
                <Component store={mocks.moderationErrors.setOneAgency} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(descriptionTextAreaSelector, ', как тебе такое, Илон Маск?');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.focus(foundationDateInputSelector);
            await page.keyboard.press('Backspace');
            await page.keyboard.press('Backspace');
            await page.type(foundationDateInputSelector, '04');
            // клик для того, чтобы скрыть дейтпикер
            await page.mouse.click(0, 0);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.hover(logoPreviewSelector);
            await page.click(logoPreviewDeleteButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(isEnabledTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('enable profile warning is disabled for approved profile with errors', async() => {
            await render(
                <Component store={mocks.enableProfileWarning} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
