import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AudioPlayer } from '../index';

const [WIDTH, HEIGHT] = [1000, 400];
const fullDownloadProgress = [
    {
        left: 0,
        width: 100,
    },
];

const Component = (props: Partial<React.ComponentProps<typeof AudioPlayer>>) => (
    <div style={{ padding: '20px' }}>
        <AudioPlayer
            duration={200}
            togglePlay={noop}
            isPaused={true}
            currentTimeProgress="30"
            downloadProgress={fullDownloadProgress}
            handleChangePosition={noop}
            currentTime={123}
            canPlay
            downloadUrl=""
            isSeek={false}
            {...props}
        />
    </div>
);

describe('AudioPlayer', () => {
    it('Предазгружено на половину', async () => {
        const downloadProgress = [
            {
                left: 0,
                width: 50,
            },
        ];

        const component = <Component downloadProgress={downloadProgress} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Предзагружено два интервала', async () => {
        const downloadProgress = [
            {
                left: 0,
                width: 50,
            },
            {
                left: 80,
                width: 10,
            },
        ];

        const component = <Component downloadProgress={downloadProgress} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Не определена продолжительность', async () => {
        const component = <Component duration={null} currentTime={null} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Плеер играет', async () => {
        const component = <Component isPaused={false} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Проигрывание заблокировано', async () => {
        const component = <Component canPlay={false} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
