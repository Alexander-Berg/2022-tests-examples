import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import ClientProfileNoteContainer from '../container';
import styles from '../styles.module.css';

const defaultStoreMock = {
    client: {
        profile: {
            data: {
                note: '123'
            },
            network: {
                bindNoteStatus: 'loaded'
            }
        }
    }
};

const note = defaultStoreMock.client.profile.data.note;

const textAreaSelector = `.${styles.textArea}`;
const cancelButtonSelector = `.${styles.actions} .awesome-icon_icon_ban`;
const savelButtonSelector = `.${styles.actions} .awesome-icon_icon_floppy-o`;

const typedText = '321';

const Component = ({ note: propsNote, store }) => (
    <AppProviders store={store}>
        <ClientProfileNoteContainer note={propsNote} />
    </AppProviders>
);

describe('ClientProfileNote', () => {
    it('correct draw typed text and reset it to default note', async() => {
        await render(<Component note={note} store={defaultStoreMock} />, { viewport: { width: 550, height: 200 } });

        await page.type(textAreaSelector, typedText);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(cancelButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw note without action buttons', async() => {
        await render(<Component note={note} store={defaultStoreMock} />, { viewport: { width: 550, height: 200 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw error message when failed', async() => {
        await render(<Component note={note} store={defaultStoreMock} />, { viewport: { width: 550, height: 220 } });

        await page.type(textAreaSelector, typedText);

        await page.click(savelButtonSelector);

        await page.waitFor(100);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    // todo кейс с успешным сохранением, проверить, что экшн кнопки пропали после клика на сохранение
});
