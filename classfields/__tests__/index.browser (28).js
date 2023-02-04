import React from 'react';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import { ClientProfileWizardAliasesContainer } from '../container';
import styles from '../styles.module.css';

const defaultStoreMock = {
    config: {
        serverTime: new Date('2020-08-05'),
        timeDelta: 0
    },
    user: {},
    client: {
        common: {
            data: {
                userType: 'AGENT',
                name: 'Иван Барашкин'
            }
        },
        profileWizardAliases: {
            data: {
                aliases: []
            },
            network: {
                getProfileWizardAliasesStatus: 'loaded',
                saveProfileWizardAliasesStatus: 'loaded'
            }
        }
    }
};

const context = {
    router: {
        entries: [ { page: 'clientProfileWizardAliases', params: { clientId: '1337' } } ]
    }
};

const textAreaSelector = `.${styles.addingInputs} .TextArea`;
const addAliasButton = `.${styles.addAlias}`;
const saveAliasesButton = `.${styles.saveAliases}`;
const deleteFirstAliasButton = `.${styles.aliasesList} .${styles.tagContainer}:first-of-type button`;
const saveAliasesButtonInModal = `.${styles.modal} .Button:first-of-type`;
const cancelSaveAliasesButtonInModal = `.${styles.modal} .Button:last-of-type`;

const Component = ({ store }) => (
    <AppProviders store={store} context={context}>
        <ClientProfileWizardAliasesContainer />
    </AppProviders>
);

describe('ClientProfileWizardAliases', () => {
    it('Корректно отрисовывет незаполненное состояние', async() => {
        await render(<Component store={defaultStoreMock} />, { viewport: { width: 750, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Корректно отрисовывет с очень длинным алиасом', async() => {
        const mock = merge({}, defaultStoreMock, {
            client: {
                profileWizardAliases: {
                    data: {
                        aliases: [
                            'kodamsokdmasoddas dl,mvfelkmv krenmckjwmnjerwn' +
                            ' rjnijmcjmc kmcoakmcijrnehnhjnc oaxmclk akmckods ' +
                            'mkjefrnijec saokdm odnfroe mo mcksam pcmeorimrijomodkam;kmcpk'
                        ]
                    }
                }
            }
        });

        await render(<Component store={mock} />, { viewport: { width: 750, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Корректно отрисовывет с множеством алиасов', async() => {
        const mock = merge({}, defaultStoreMock, {
            client: {
                profileWizardAliases: {
                    data: {
                        aliases: [
                            '123',
                            '3131231',
                            'dsad a',
                            '313d as dsa1231',
                            '313a d as1231',
                            '3131d adas231',
                            '3131 asd 313as231',
                            '3131  3sadas231',
                            '3d 3sadacr432s231',
                            '3131 asd 3sewdadas231',
                            '313sadas231',
                            '3sd 3sadas231',
                            '3asd 3sadas231'
                        ]
                    }
                }
            }
        });

        await render(<Component store={mock} />, { viewport: { width: 750, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Корректно заполняет, добавляет и удаляет алиасы', async() => {
        await render(<Component store={defaultStoreMock} />, { viewport: { width: 750, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type(textAreaSelector, 'Агентство недвижимости этажи');

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(addAliasButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(deleteFirstAliasButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('Корректно выбрасывает ошибку при сохранении алиасов', async() => {
        const mock = merge({}, defaultStoreMock, {
            client: {
                profileWizardAliases: {
                    data: {
                        aliases: [
                            'Первый',
                            'Второй'
                        ]
                    }
                }
            }
        });

        await render(<Component store={mock} />, { viewport: { width: 750, height: 500 } });

        await page.click(deleteFirstAliasButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(saveAliasesButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(saveAliasesButtonInModal);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Корректно закрывает попап при клике на кнопку "Нет" в модальном окне', async() => {
        const mock = merge({}, defaultStoreMock, {
            client: {
                profileWizardAliases: {
                    data: {
                        aliases: [ 'Первый' ]
                    }
                }
            }
        });

        await render(<Component store={mock} />, { viewport: { width: 750, height: 500 } });

        await page.click(deleteFirstAliasButton);

        await page.click(saveAliasesButton);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(cancelSaveAliasesButtonInModal);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
