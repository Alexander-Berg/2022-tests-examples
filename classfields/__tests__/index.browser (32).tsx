import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MaskedInput } from '../';

const mobileViewports = [{ width: 360, height: 200 }] as const;
const desktopViewports = [] as const;

const viewports = [...mobileViewports, ...desktopViewports] as const;

const render = async (component: React.ReactElement) => {
    for (const viewport of viewports) {
        await _render(component, { viewport });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

describe('MaskedInput', () => {
    it('рендерится корректно', async () => {
        const Component = () => {
            const [value, setState] = React.useState<string>('4004555444');

            const eventHandler = (value: string | undefined) => setState(value || '');

            return (
                <MaskedInput
                    separator=" - "
                    separatorPositions={[3]}
                    id="input"
                    variant="bordered"
                    size="l"
                    label="Серия и номер паспорта"
                    value={value}
                    onChange={eventHandler}
                    type="tel"
                    maxLength={13}
                />
            );
        };

        await render(<Component />);
    });
});
