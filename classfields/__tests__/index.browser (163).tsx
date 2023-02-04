import React from 'react';
import { render } from 'jest-puppeteer-react';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';
import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PaidReportStatus } from 'realty-core/view/react/common/types/egrnPaidReport';

import { EgrnReport } from '../index';
import { IEgrnReportProps } from '../types';
import styles from '../styles.module.css';

const SIZES = [
    { width: 1000, height: 400 },
    { width: 1400, height: 400 },
];

const renderSeveralResolutions = async (Component: React.ReactElement, dimensions: typeof SIZES) => {
    for (const dimension of dimensions) {
        await render(Component, {
            viewport: dimension,
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

// eslint-disable-next-line
const Component: React.FunctionComponent<Partial<IEgrnReportProps>> = (props: any) => (
    <EgrnReport
        offerId="9024018379422454529"
        paidReportId="d06c2115ed04b31aeb1e71c30733483"
        isOfferAccessibleByLink
        address="Центральный проезд Хорошевсского Серебрянного Бора, 12"
        reportDate="2020-08-19T08:13:32.396Z"
        area={31.1}
        floor="3"
        cadastralNumber="78:11:0006040:3389"
        {...props}
    />
);

describe('EgrnReport', () => {
    describe('Состояния отчета', () => {
        it('Отчет в статусе DONE, без ховера', async () => {
            await renderSeveralResolutions(<Component reportStatus={PaidReportStatus.DONE} />, SIZES);
        });

        it('Отчет в статусе NEW', async () => {
            await renderSeveralResolutions(<Component reportStatus={PaidReportStatus.NEW} />, SIZES);
        });

        it('Отчет в статусе IN_PROGRESS', async () => {
            await renderSeveralResolutions(<Component reportStatus={PaidReportStatus.IN_PROGRESS} />, SIZES);
        });

        it('Отчет в статусе ERROR', async () => {
            await renderSeveralResolutions(<Component reportStatus={PaidReportStatus.ERROR} />, SIZES);
        });
    });

    describe('Другие состояния', () => {
        it('Отчет в статусе DONE, с ховером', async () => {
            await render(<Component reportStatus={PaidReportStatus.DONE} />, {
                viewport: SIZES[0],
            });

            await page.hover(`.${styles.snippet}`);

            expect(
                await takeScreenshot({
                    keepCursor: true,
                })
            ).toMatchImageSnapshot();
        });

        it('Нет по площади и этажу', async () => {
            await renderSeveralResolutions(
                <Component reportStatus={PaidReportStatus.DONE} floor={undefined} area={undefined} />,
                SIZES
            );
        });

        it('Нет по площади, этажу и кадастровому номеру', async () => {
            await renderSeveralResolutions(
                <Component
                    reportStatus={PaidReportStatus.DONE}
                    floor={undefined}
                    area={undefined}
                    cadastralNumber={undefined}
                />,
                SIZES
            );
        });

        it('Длинный адрес', async () => {
            await renderSeveralResolutions(
                <Component
                    reportStatus={PaidReportStatus.DONE}
                    /* eslint-disable-next-line max-len */
                    address="Центральный проезд Хорошевсского Серебрянного Бора имени Хороша, как вы, наверное, поняли, 12, кв 743, странная дверь налево"
                />,
                SIZES
            );
        });

        it('Есть фото', async () => {
            await renderSeveralResolutions(
                <Component
                    reportStatus={PaidReportStatus.DONE}
                    imageUrl={generateImageUrl({ width: 800, height: 600 })}
                />,
                SIZES
            );
        });

        it('Оффер не доступен по ссылке', async () => {
            await renderSeveralResolutions(
                <Component reportStatus={PaidReportStatus.DONE} isOfferAccessibleByLink={false} />,
                SIZES
            );
        });

        it('Длинный адрес и длинная площадь', async () => {
            await renderSeveralResolutions(
                <Component
                    reportStatus={PaidReportStatus.DONE}
                    /* eslint-disable-next-line max-len */
                    address="Центральный проезд Хорошевсского Серебрянного Бора имени Хороша, как вы, наверное, поняли, 12, кв 743, странная дверь налево"
                    area={1200.54}
                />,
                SIZES
            );
        });
    });
});
