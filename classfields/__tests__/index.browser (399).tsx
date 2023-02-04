import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject } from 'realty-core/types/utils';

import { OfferCardUserNoteContainer } from '../container';
import styles from '../styles.module.css';

import { offer, state } from './mocks';

const Component: React.FC<{ note?: string; Gate?: AnyObject }> = ({ note, Gate }) => {
    return (
        <AppProvider Gate={Gate} initialState={state}>
            <OfferCardUserNoteContainer offer={{ ...offer, userNote: note }} placement="blank" eventPlace="blank" />
        </AppProvider>
    );
};

describe('OfferCardUserNote', () => {
    it('Отрисовка в состоянии превью. Пустая заметка', async () => {
        await render(<Component />, {
            viewport: { width: 400, height: 200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка в состоянии превью. Заметка в одну строку', async () => {
        await render(<Component note="Заметка" />, {
            viewport: { width: 400, height: 200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка в состоянии превью. Заметка в во много строк', async () => {
        await render(<Component note={'Заметка\n'.repeat(7)} />, {
            viewport: { width: 400, height: 200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка в состоянии превью. Очень длинное слово', async () => {
        await render(<Component note="ОченьДлиннаяЗаметкаВОдноСловоДолжнаПереноситьсяНаНовуюСтроку" />, {
            viewport: { width: 400, height: 200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Редактирование заметки', async () => {
        await render(<Component />, {
            viewport: { width: 400, height: 200 },
        });

        await page.click(`.${styles.notePreview}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type('textarea', 'aaa\naaa\n');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type('textarea', 'bbb\nbbb\n');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Редактирование заметки. Длинное слово', async () => {
        await render(<Component />, {
            viewport: { width: 400, height: 200 },
        });

        await page.click(`.${styles.notePreview}`);

        await page.type('textarea', 'ОченьДлиннаяЗаметкаВОдноСловоДолжнаПереноситьсяНаНовуюСтроку');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Успешное удаление заметки', async () => {
        await render(
            <Component
                Gate={{
                    create: () => Promise.resolve(true),
                }}
                note={'Заметка'}
            />,
            {
                viewport: { width: 400, height: 200 },
            }
        );
        await page.click(`.${styles.notePreview}`);
        await page.click(`.${styles.delete}`);
        await page.waitForSelector(`.${styles.notePreview}`, { visible: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Неуспешное удаление заметки', async () => {
        await render(
            <Component
                Gate={{
                    create: () => Promise.reject(),
                }}
                note={'Заметка'}
            />,
            {
                viewport: { width: 400, height: 200 },
            }
        );

        await page.click(`.${styles.notePreview}`);
        await page.click(`.${styles.delete}`);
        await page.waitForSelector(`.${styles.saveErrorMessage}`, { visible: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Успешное сохранение заметки', async () => {
        await render(
            <Component
                Gate={{
                    create: () => Promise.resolve(true),
                }}
            />,
            {
                viewport: { width: 400, height: 200 },
            }
        );
        await page.click(`.${styles.notePreview}`);
        await page.type('textarea', 'aaaa');
        await page.click(`.${styles.save}`);
        await page.waitForSelector(`.${styles.notePreview}`, { visible: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Неуспешное сохранение заметки', async () => {
        await render(
            <Component
                Gate={{
                    create: () => Promise.reject(),
                }}
                note={'Заметка'}
            />,
            {
                viewport: { width: 400, height: 200 },
            }
        );

        await page.click(`.${styles.notePreview}`);
        await page.type('textarea', 'aaaa');
        await page.click(`.${styles.save}`);
        await page.waitForSelector(`.${styles.saveErrorMessage}`, { visible: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
