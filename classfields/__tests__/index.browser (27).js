import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import ClientProfileUsersContainer from '../container';
import styles from '../styles.module.css';

const defaultStoreMock = {
    user: {
        permissions: [ 'edit_any_client' ]
    },
    client: {
        profile: {
            data: {
                users: [
                    { name: 'roman', uid: '1337' },
                    { name: 'andrey', uid: '1338' },
                    { name: 'jeka', uid: '1339' }
                ]
            },
            network: {
                bindUsersStatus: 'loaded'
            }
        }
    }
};

const users = defaultStoreMock.client.profile.data.users;

const editButtonSelector = `.${styles.actions} .awesome-icon_icon_pencil`;
const cancelButtonSelector = `.${styles.actions} .awesome-icon_icon_ban`;
const saveButtonSelector = `.${styles.actions} .awesome-icon_icon_floppy-o`;
const lastTagDeleteButtonSelector = `.${styles.suggest} ul li:last-of-type button`;

const Component = ({ users: propsUsers, store }) => (
    <AppProviders store={store}>
        <ClientProfileUsersContainer users={propsUsers} />
    </AppProviders>
);

describe('ClientProfileUsers', () => {
    it('correct draw view mode', async() => {
        await render(
            <Component users={users} store={defaultStoreMock} />,
            { viewport: { width: 500, height: 90 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw without permissions to edit', async() => {
        const mock = { ...defaultStoreMock, user: { permissions: [] } };

        await render(
            <Component users={users} store={mock} />,
            { viewport: { width: 500, height: 90 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw edit mode', async() => {
        await render(
            <Component users={users} store={defaultStoreMock} />,
            { viewport: { width: 500, height: 90 } }
        );

        await page.click(editButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw tags when it reset to defaults', async() => {
        await render(
            <Component users={users} store={defaultStoreMock} />,
            { viewport: { width: 500, height: 90 } }
        );

        await page.click(editButtonSelector);

        await page.click(lastTagDeleteButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(cancelButtonSelector);

        await page.click(editButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw error message when failed', async() => {
        await render(
            <Component users={users} store={defaultStoreMock} />,
            { viewport: { width: 500, height: 90 } }
        );

        await page.click(editButtonSelector);

        await page.click(lastTagDeleteButtonSelector);

        await page.click(saveButtonSelector);

        await page.waitFor(100);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    // todo кейс с успешным сохранением, проверить, что экшн проищошел переход во view mode
});
