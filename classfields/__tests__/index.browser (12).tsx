import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import waterHot48 from '@realty-front/icons/arenda/water-hot-48.svg';

import { ImageContainerIconType } from 'view/components/BaseSnippetImageContainer';

import { HouseServiceSnippet } from '../index';
import styles from '../styles.module.css';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 580, height: 300 } }];

describe('HouseServiceSnippet', () => {
    describe(`С одним заголовком`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <HouseServiceSnippet
                        title={'Счётчики'}
                        onClick={() => {
                            return;
                        }}
                        icon={waterHot48}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('С заголовком и описанием', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <HouseServiceSnippet
                        title={'Счётчики'}
                        description1={'В ванной комнате'}
                        onClick={() => {
                            return;
                        }}
                        icon={waterHot48}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('С заголовком и 2 описаниями', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <HouseServiceSnippet
                        title={'Счётчики'}
                        description1={'В ванной комнате'}
                        description2={'1000 Р'}
                        onClick={() => {
                            return;
                        }}
                        icon={waterHot48}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Переполнение в описаниях', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <HouseServiceSnippet
                        title={'Счётчики'}
                        description1={
                            // eslint-disable-next-line max-len
                            'Переполнение, переполнение, переполнение, переполнение, переполнение, переполнение, переполнение, переполнение, переполнение, переполнение'
                        }
                        description2={
                            // eslint-disable-next-line max-len
                            'Переполнение, переполнение, переполнение, переполнение, переполнение, переполнение, переполнение, переполнение, переполнение, переполнение'
                        }
                        onClick={() => {
                            return;
                        }}
                        icon={waterHot48}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    it('Ховер в десктопе', async () => {
        await render(
            <HouseServiceSnippet
                title={'Счётчики'}
                description1={'В ванной комнате'}
                description2={'1000 Р'}
                onClick={() => {
                    return;
                }}
                icon={waterHot48}
            />,
            renderOptions[0]
        );

        await page.hover(`.${styles.info}`);

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });

    it('Тип иконки - ошибка', async () => {
        await render(
            <HouseServiceSnippet
                title={'Счётчики'}
                description1={'В ванной комнате'}
                description2={'1000 Р'}
                onClick={() => {
                    return;
                }}
                icon={waterHot48}
                iconType={ImageContainerIconType.ERROR}
            />,
            renderOptions[0]
        );

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });

    it('Тип иконки - варнинг', async () => {
        await render(
            <HouseServiceSnippet
                title={'Счётчики'}
                description1={'В ванной комнате'}
                description2={'1000 Р'}
                onClick={() => {
                    return;
                }}
                icon={waterHot48}
                iconType={ImageContainerIconType.WARNING}
            />,
            renderOptions[0]
        );

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });

    it('Тип иконки - успех', async () => {
        await render(
            <HouseServiceSnippet
                title={'Счётчики'}
                description1={'В ванной комнате'}
                description2={'1000 Р'}
                onClick={() => {
                    return;
                }}
                icon={waterHot48}
                iconType={ImageContainerIconType.SUCCESS}
            />,
            renderOptions[0]
        );

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });

    it(`Два соседних сниппета в таче`, async () => {
        await render(
            <>
                <HouseServiceSnippet
                    title={'Счётчики'}
                    onClick={() => {
                        return;
                    }}
                    icon={waterHot48}
                />
                <HouseServiceSnippet
                    title={'Счётчики'}
                    onClick={() => {
                        return;
                    }}
                    icon={waterHot48}
                />
            </>,
            renderOptions[1]
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
