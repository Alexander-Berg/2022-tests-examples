import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IUserTenantQuestionnaire, UserPersonalActivity } from 'types/user';

import 'view/styles/common.css';

import { AppProvider } from 'view/libs/test-helpers';
import { userReducer } from 'view/entries/user/reducer';

import { OwnerFlatTenantQuestionnairePreview } from '../index';

import { store } from './stubs';

const renderOptions = [
    { viewport: { width: 960, height: 1200 } },
    { viewport: { width: 625, height: 1200 } },
    { viewport: { width: 360, height: 1200 } },
];

const Component: React.FunctionComponent<{ tenantQuestionnaire: Partial<IUserTenantQuestionnaire> }> = (props) => (
    <AppProvider initialState={store} rootReducer={userReducer} bodyBackgroundColor={AppProvider.PageColor.USER_LK}>
        <OwnerFlatTenantQuestionnairePreview tenantQuestionnaire={props.tenantQuestionnaire} />
    </AppProvider>
);

describe('OwnerFlatTenantQuestionnairePreview', () => {
    describe('Анкета не заполнена', () => {
        renderOptions.forEach((renderOption) => {
            const tenantQuestionnaire = {};

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component tenantQuestionnaire={tenantQuestionnaire} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Учится', () => {
        renderOptions.forEach((renderOption) => {
            const tenantQuestionnaire = {
                personalActivity: {
                    activity: UserPersonalActivity.STUDY,
                    educationalInstitution: 'СПБГЭТУ "ЛЭТИ"',
                    aboutWorkAndPosition: '',
                    aboutBusiness: '',
                },
            };

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component tenantQuestionnaire={tenantQuestionnaire} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Работает', () => {
        renderOptions.forEach((renderOption) => {
            const tenantQuestionnaire = {
                personalActivity: {
                    activity: UserPersonalActivity.WORK,
                    educationalInstitution: '',
                    aboutWorkAndPosition: 'ООО Яндекс Вертикали, MC',
                    aboutBusiness: '',
                },
            };

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component tenantQuestionnaire={tenantQuestionnaire} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Работает и учится', () => {
        renderOptions.forEach((renderOption) => {
            const tenantQuestionnaire = {
                personalActivity: {
                    activity: UserPersonalActivity.WORK_AND_STUDY,
                    educationalInstitution: 'СПБГЭТУ "ЛЭТИ"',
                    aboutWorkAndPosition: 'ООО Яндекс Вертикали, MC',
                    aboutBusiness: '',
                },
            };

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component tenantQuestionnaire={tenantQuestionnaire} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Бизнес', () => {
        renderOptions.forEach((renderOption) => {
            const tenantQuestionnaire = {
                personalActivity: {
                    activity: UserPersonalActivity.BUSINESS_OWNER,
                    educationalInstitution: '',
                    aboutWorkAndPosition: '',
                    aboutBusiness: 'Торгую яблоками и людьми',
                },
            };

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component tenantQuestionnaire={tenantQuestionnaire} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Домохозяйка', () => {
        renderOptions.forEach((renderOption) => {
            const tenantQuestionnaire = {
                personalActivity: {
                    activity: UserPersonalActivity.HOMEBODY,
                    educationalInstitution: '',
                    aboutWorkAndPosition: '',
                    aboutBusiness: '',
                },
            };

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component tenantQuestionnaire={tenantQuestionnaire} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Безработный', () => {
        renderOptions.forEach((renderOption) => {
            const tenantQuestionnaire = {
                personalActivity: {
                    activity: UserPersonalActivity.UNEMPLOYED,
                    educationalInstitution: '',
                    aboutWorkAndPosition: '',
                    aboutBusiness: '',
                },
            };

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component tenantQuestionnaire={tenantQuestionnaire} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
