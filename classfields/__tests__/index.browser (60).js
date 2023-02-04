import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { WithScrollContextProvider } from 'realty-core/view/react/common/enhancers/withScrollContext';

import { AppProvider } from 'view/lib/test-helpers';

import SettingsContactsContainer from '../container';

import mocks from './mocks';

const Component = ({ store, isProfileAvailable = false }) => (
    <AppProvider initialState={store}>
        <WithScrollContextProvider offest={0}>
            <SettingsContactsContainer isProfileAvailable={isProfileAvailable} />
        </WithScrollContextProvider>
    </AppProvider>
);

const userTypeButtonSelectorFactory = n => `#settings_contacts_user_type .Radio:nth-child(${n})`;
const phoneSelectorFactory = n => `#settings_contacts_phones_${n} .TextInput__control`;
const phoneTumblerSelectorFactory = n => `#settings_contacts_phones_${n} .Tumbler`;
const activePhoneButtonSelectorFactory = n => `#settings_contacts_phones_${n} .Button`;
const addPhoneSelector = '#settings_contacts_phones_add .Button';
const editingPhoneWithConfirmationSelector = '#settings_contacts_phones_editing_phone .TextInput__control';
const editingPhoneWithConfirmationDeleteButtonSelector = '#settings_contacts_phones_editing_phone .Button';
const trademarkSelector = '#settings_contacts_trademark .Tag';
const nameInputSelector = '#settings_contacts_name .TextInput__control';
const emailInputSelector = '#settings_contacts_email .TextInput__control';
const ogrnInputSelector = '#settings_contacts_ogrn .TextInput__control';
const redirectPhonesTumblerSelector = '#settings_contacts_redirect_phones .Tumbler__button';

describe('SettingsContacts', () => {
    describe('view', () => {
        it('owner default', async() => {
            await render(
                <Component store={mocks.owner.default} />,
                { viewport: { width: 1000, height: 750 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('owner with Mosru section', async() => {
            await render(
                <Component store={mocks.owner.mosru} />,
                { viewport: { width: 1000, height: 1000 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('owner filled', async() => {
            await render(
                <Component store={mocks.owner.filled} />,
                { viewport: { width: 1000, height: 750 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agent natural default', async() => {
            await render(
                <Component store={mocks.agent.defaultNatural} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agent natural filled', async() => {
            await render(
                <Component store={mocks.agent.filledNatural} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agent juridical default', async() => {
            await render(
                <Component store={mocks.agent.defaultJuridical} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agent juridical filled', async() => {
            await render(
                <Component store={mocks.agent.filledJuridical} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agency default', async() => {
            await render(
                <Component store={mocks.agency.default} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agency filled', async() => {
            await render(
                <Component store={mocks.agency.filled} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agency filled with loaded trademark', async() => {
            await render(
                <Component store={mocks.agency.withLoadedTrademark} isProfileAvailable />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(trademarkSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('developer default', async() => {
            await render(
                <Component store={mocks.developer.default} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('developer filled', async() => {
            await render(
                <Component store={mocks.developer.filled} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('developer overrides profile errors', async() => {
            await render(
                <Component store={mocks.developer.withModerationMessages} />,
                { viewport: { width: 1000, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('errors', () => {
        describe('validation', () => {
            it('not filled 1000', async() => {
                await render(
                    <Component store={mocks.errors.validation.notFilled} />,
                    { viewport: { width: 1000, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(userTypeButtonSelectorFactory(2));

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(userTypeButtonSelectorFactory(3));

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(userTypeButtonSelectorFactory(4));

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('not filled 1400', async() => {
                await render(
                    <Component store={mocks.errors.validation.notFilled} />,
                    { viewport: { width: 1400, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(userTypeButtonSelectorFactory(2));

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(userTypeButtonSelectorFactory(3));

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(userTypeButtonSelectorFactory(4));

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });

        describe('moderation', () => {
            it('wrong name and no answer 1000', async() => {
                await render(
                    <Component store={mocks.errors.moderation.wrongNameAndNoAnswer} />,
                    { viewport: { width: 1000, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(userTypeButtonSelectorFactory(2));

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('wrong name and no answer 1400', async() => {
                await render(
                    <Component store={mocks.errors.moderation.wrongNameAndNoAnswer} />,
                    { viewport: { width: 1400, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(userTypeButtonSelectorFactory(2));

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('stolen name 1000', async() => {
                await render(
                    <Component store={mocks.errors.moderation.stolenName} isProfileAvailable />,
                    { viewport: { width: 1000, height: 900 } }
                );

                await page.click(userTypeButtonSelectorFactory(2));

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('stolen name 1400', async() => {
                await render(
                    <Component store={mocks.errors.moderation.stolenName} isProfileAvailable />,
                    { viewport: { width: 1400, height: 900 } }
                );

                await page.click(userTypeButtonSelectorFactory(2));

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('name not registered 1000', async() => {
                await render(
                    <Component store={mocks.errors.moderation.nameNotRegistered} isProfileAvailable />,
                    { viewport: { width: 1000, height: 900 } }
                );

                await page.click(userTypeButtonSelectorFactory(2));

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('name not registered 1400', async() => {
                await render(
                    <Component store={mocks.errors.moderation.nameNotRegistered} />,
                    { viewport: { width: 1400, height: 900 } }
                );

                await page.click(userTypeButtonSelectorFactory(2));

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });

        describe('backend', () => {
            it('duplicate name 1000', async() => {
                await render(
                    <Component store={mocks.errors.backend.duplicateName} />,
                    { viewport: { width: 1000, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('duplicate name 1400', async() => {
                await render(
                    <Component store={mocks.errors.backend.duplicateName} />,
                    { viewport: { width: 1400, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('duplicate ogrn 1000', async() => {
                await render(
                    <Component store={mocks.errors.backend.duplicateOgrn} />,
                    { viewport: { width: 1000, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('duplicate ogrn 1400', async() => {
                await render(
                    <Component store={mocks.errors.backend.duplicateOgrn} />,
                    { viewport: { width: 1400, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });

        describe('custom', () => {
            it('trademark uploading failed 1000', async() => {
                await render(
                    <Component store={mocks.errors.custom.trademarkUploadingFailed} isProfileAvailable />,
                    { viewport: { width: 1000, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('trademark uploading failed 1400', async() => {
                await render(
                    <Component store={mocks.errors.custom.trademarkUploadingFailed} isProfileAvailable />,
                    { viewport: { width: 1400, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('trademark too large 1000', async() => {
                await render(
                    <Component store={mocks.errors.custom.trademarkTooLarge} isProfileAvailable />,
                    { viewport: { width: 1000, height: 900 } }
                );

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(trademarkSelector);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('logic', () => {
        it('correct behavior on blur and on change events with moderation error', async() => {
            await render(
                <Component store={mocks.errors.moderation.wrongNameAndNoAnswer} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.focus(nameInputSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.$eval(nameInputSelector, e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(nameInputSelector, '1');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('show validation on blur name', async() => {
            await render(
                <Component store={mocks.owner.default} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.focus(nameInputSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.$eval(nameInputSelector, e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(nameInputSelector, '1');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.$eval(nameInputSelector, e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('show validation on blur email', async() => {
            await render(
                <Component store={mocks.owner.default} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.focus(emailInputSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.$eval(emailInputSelector, e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(emailInputSelector, '13332@list');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.$eval(emailInputSelector, e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('correct works redirect phones tumbler', async() => {
            await render(
                <Component store={mocks.owner.default} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.click(redirectPhonesTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(redirectPhonesTumblerSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('correct change user type', async() => {
            await render(
                <Component store={mocks.owner.default} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.click(userTypeButtonSelectorFactory(2));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(userTypeButtonSelectorFactory(3));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('correct show validation agent ogrn', async() => {
            await render(
                <Component store={mocks.agent.defaultNatural} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.type(ogrnInputSelector, '1177746');

            await page.$eval(ogrnInputSelector, e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(ogrnInputSelector, '415857');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('correct show validation agency ogrn', async() => {
            await render(
                <Component store={mocks.agency.default} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.type(ogrnInputSelector, '1177746');

            await page.$eval(ogrnInputSelector, e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(ogrnInputSelector, '415857');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('agency and agent ogrn sync', async() => {
            await render(
                <Component store={mocks.agent.defaultNatural} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.type(ogrnInputSelector, '09876543');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(userTypeButtonSelectorFactory(2));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('manipulations with phones with confirmation', async() => {
            await render(
                <Component store={mocks.owner.filled} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(addPhoneSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(editingPhoneWithConfirmationSelector, '1177746');
            await page.$eval(editingPhoneWithConfirmationSelector, e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(editingPhoneWithConfirmationDeleteButtonSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(phoneTumblerSelectorFactory(0));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('manipulations with phones without confirmation', async() => {
            await render(
                <Component store={mocks.agency.filled} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.focus(phoneSelectorFactory(0));
            await page.keyboard.press('Backspace');
            await page.$eval(phoneSelectorFactory(0), e => e.blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(activePhoneButtonSelectorFactory(0));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(addPhoneSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(phoneSelectorFactory(1), '79508881302');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(activePhoneButtonSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('hide no answer moderation error on phone when phone tumbler check', async() => {
            await render(
                <Component store={mocks.errors.moderation.wrongNameAndNoAnswer} />,
                { viewport: { width: 1400, height: 900 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(phoneTumblerSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('hide no answer moderation error on phone change', async() => {
            await render(
                <Component store={mocks.errors.moderation.wrongNameAndNoAnswer} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.click(userTypeButtonSelectorFactory(2));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.focus(phoneSelectorFactory(1));
            await page.keyboard.press('Backspace');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('hide no answer moderation error on phone delete', async() => {
            await render(
                <Component store={mocks.errors.moderation.wrongNameAndNoAnswer} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.click(userTypeButtonSelectorFactory(2));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(activePhoneButtonSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('hide no answer moderation error on phone add', async() => {
            await render(
                <Component store={mocks.errors.moderation.wrongNameAndNoAnswer} />,
                { viewport: { width: 1400, height: 900 } }
            );

            await page.click(userTypeButtonSelectorFactory(2));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(addPhoneSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
