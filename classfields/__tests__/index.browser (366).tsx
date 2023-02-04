import React from 'react';
import { render } from 'jest-puppeteer-react';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';
import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import sliderStyles from 'view/react/deskpad/components/CardSlider/styles.module.css';

import { OfferCardBlogPosts } from '..';
import blogPostStyles from '../styles.module.css';

const blogPost = {
    src: generateImageUrl({ width: 1200, height: 1200 }),
    tag: 'Новости',
    title: 'Сколько стоит ваша квартира (или не ваша). Как объективно оценить жильё',
    url: '/journal/post/skolko-stoit-vasha-kvartira-ili-ne-vasha-.-kak-obektivno-ocenit-zhilyo/?from=offer',
};

const blogPosts = Array(4).fill(blogPost);

const renderComponent = () =>
    render(
        <AppProvider>
            <OfferCardBlogPosts linkFrom="offer" blogPosts={blogPosts} />
        </AppProvider>,
        { viewport: { width: 900, height: 500 } }
    );

describe('OfferCardBlogPosts', () => {
    it('рисует блок', async () => {
        await renderComponent();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('отрабатывает ховер на пост', async () => {
        await renderComponent();

        await page.hover(`.${blogPostStyles.post}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('отрабатывает клики по контролам слайдера', async () => {
        await renderComponent();

        await page.click(`.${sliderStyles.nextButton}`);
        await page.waitFor(200);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${sliderStyles.prevButton}`);
        await page.waitFor(200);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
