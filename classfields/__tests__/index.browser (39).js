import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import RaisingBuyButton from '../index';

const DEFAULT_WIDTH = 390;
const DEFAULT_HEIGHT = 78;

const Button = props => <RaisingBuyButton {...props} />;

const Container = ({ width = 350, children }) => (
    <div style={{ width: `${width}px` }}>
        {children}
    </div>
);

describe('RaisingBuyButton', () => {
    it('without loading, product active, without discount', async() => {
        await render(
            <Container>
                <Button
                    isLoading={false}
                    isActive
                    price={666}
                    basePrice={666}
                />
            </Container>, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('without loading, product inactive, without discount', async() => {
        await render(
            <Container>
                <Button
                    isLoading={false}
                    isActive={false}
                    price={666}
                    basePrice={666}
                />
            </Container>, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('with loading, product inactive, without discount', async() => {
        await render(
            <Container>
                <Button
                    isLoading
                    isActive={false}
                    price={666}
                    basePrice={666}
                />
            </Container>, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('without loading, product inactive, with discount', async() => {
        await render(
            <Container>
                <Button
                    isLoading={false}
                    isActive={false}
                    price={666}
                    basePrice={777}
                />
            </Container>, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('without loading, product inactive, free', async() => {
        await render(
            <Container>
                <Button
                    isLoading={false}
                    isActive={false}
                    price={0}
                    basePrice={777}
                />
            </Container>, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('without loading, product active, free', async() => {
        await render(
            <Container>
                <Button
                    isLoading={false}
                    isActive
                    price={0}
                    basePrice={777}
                />
            </Container>, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('without loading, product inactive, with discount, wide screen', async() => {
        await render(
            <Container>
                <Button
                    isLoading={false}
                    isActive={false}
                    price={666}
                    basePrice={777}
                />
            </Container>, { viewport: { width: 1150, height: DEFAULT_HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('with loading, product inactive, with discount, small screen', async() => {
        await render(
            <Container width={258}>
                <Button
                    isLoading
                    isActive={false}
                    price={666}
                    basePrice={777}
                />
            </Container>, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
