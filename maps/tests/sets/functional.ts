import {getLink, setViewportSize} from '../utils';
import {Size, Theme} from '../../src/types/custom';

const YANDEX = {
    id: 1124715036,
    seoname: 'yandeks'
};

const YANDEX_WITH_REVIEWS = {
    ...YANDEX,
    review: true
};

describe('Ссылки', () => {
    it('"Оставить отзыв"', async function () {
        await openPage(this.browser);
        await verifyLink(
            this.browser,
            '.badge__link-to-map',
            buildLink({
                org: YANDEX_WITH_REVIEWS,
                medium: 'reviews',
                content: 'add_review',
                addReview: true
            })
        );
    });

    describe('"Больше отзывов на Яндекс Картах"', () => {
        const selector = '.badge__more-reviews-link';

        it('Есть когда отзывов много', async function () {
            await openPage(this.browser);
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    org: YANDEX_WITH_REVIEWS,
                    medium: 'reviews',
                    content: 'more-reviews'
                })
            );
        });

        it('Нет когда отзывов мало', async function () {
            await openPage(this.browser, {orgId: 42483407187});
            await this.browser.waitForVisible(selector, 3000, true);
        });
    });

    describe('Ссылка на старом большом бейдже', () => {
        const selector = '.old-badge__link';

        it('На существующей организации', async function () {
            await openPage(this.browser, {isOld: true});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    org: YANDEX_WITH_REVIEWS,
                    medium: 'reviews-old-browser',
                    content: 'old-browser-check-reviews'
                })
            );
        });

        it('На несуществующей организации', async function () {
            await openPage(this.browser, {orgId: 1, isOld: true});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    medium: 'reviews-old-browser',
                    content: 'not-found'
                })
            );
        });
    });

    it('Ссылка на "ещё"', async function () {
        await openPage(this.browser);
        await verifyLink(
            this.browser,
            '.comment__read-more > a',
            buildLink({
                org: YANDEX_WITH_REVIEWS,
                medium: 'reviews',
                content: 'read-more'
            })
        );
    });

    describe('Вью ошибки', () => {
        it('"Смотрите отзывы на Яндекс Картах"', async function () {
            await openPage(this.browser, {orgId: 1210449111});
            await verifyLink(
                this.browser,
                '.error-view__link',
                buildLink({
                    org: {
                        id: 1210449111,
                        seoname: 'azbuka_vkusa',
                        review: true
                    },
                    medium: 'reviews',
                    content: 'reviews_load_error'
                })
            );
        });

        it('"Искать на Яндекс Картах"', async function () {
            await openPage(this.browser, {orgId: 1});
            await verifyLink(
                this.browser,
                '.error-view__link',
                buildLink({
                    medium: 'reviews',
                    content: 'not-found'
                })
            );
        });

        it('На логотипе', async function () {
            await openPage(this.browser, {orgId: 1});
            await verifyLink(
                this.browser,
                '.error-view__logo .logo',
                buildLink({
                    medium: 'reviews',
                    content: 'logo'
                })
            );
        });
    });

    describe('На логотипе мини-бейджа', () => {
        const selector = '.mini-badge__logo .logo';

        it('В большом размере - присутствует', async function () {
            await openPage(this.browser, {size: 'x'});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    medium: 'reviews',
                    content: 'logo'
                })
            );
        });

        it('В среднем размере - отсутствует', async function () {
            await openPage(this.browser, {size: 'm'});
            await verifyLink(this.browser, selector, null);
        });

        it('В маленьком размере - отсутствует', async function () {
            await openPage(this.browser, {size: 's'});
            await verifyLink(this.browser, selector, null);
        });

        it('В старой версии', async function () {
            await openPage(this.browser, {size: 'x', isOld: true});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    medium: 'reviews-old-browser',
                    content: 'logo'
                })
            );
        });
    });

    describe('На рейтинге мини-бейджа', () => {
        const selector = '.mini-badge__rating';

        it('В большом размере - присутствует', async function () {
            await openPage(this.browser, {size: 'x'});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    org: YANDEX_WITH_REVIEWS,
                    medium: 'reviews',
                    content: 'rating'
                })
            );
        });

        it('В среднем размере - отсутствует', async function () {
            await openPage(this.browser, {size: 'm'});
            await verifyLink(this.browser, selector, null);
        });

        it('В маленьком размере - отсутствует', async function () {
            await openPage(this.browser, {size: 's'});
            await this.browser.waitForVisible(selector, 3000, true);
        });

        it('В старой версии', async function () {
            await openPage(this.browser, {size: 'x', isOld: true});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    org: YANDEX_WITH_REVIEWS,
                    medium: 'reviews-old-browser',
                    content: 'rating'
                })
            );
        });
    });

    describe('На заголовке мини-бейджа', () => {
        const selector = '.mini-badge__org-name';

        it('В большом размере - присутствует', async function () {
            await openPage(this.browser, {size: 'x'});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    org: YANDEX,
                    medium: 'reviews',
                    content: 'org-name'
                })
            );
        });

        it('В среднем размере - отсутствует', async function () {
            await openPage(this.browser, {size: 'm'});
            await verifyLink(this.browser, selector, null);
        });

        it('В маленьком размере - отсутствует', async function () {
            await openPage(this.browser, {size: 's'});
            await verifyLink(this.browser, selector, null);
        });

        it('В старой версии', async function () {
            await openPage(this.browser, {size: 'x', isOld: true});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    org: YANDEX,
                    medium: 'reviews-old-browser',
                    content: 'org-name'
                })
            );
        });
    });

    describe('На мини-бейдже', () => {
        const selector = '.mini-badge';

        it('В большом размере - отсутствует', async function () {
            await openPage(this.browser, {size: 'x'});
            await verifyLink(this.browser, selector, null);
        });

        it('В среднем размере - присутствует', async function () {
            await openPage(this.browser, {size: 'm'});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    org: YANDEX,
                    medium: 'm'
                })
            );
        });

        it('В маленьком размере - присутствует', async function () {
            await openPage(this.browser, {size: 's'});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    org: YANDEX,
                    medium: 's'
                })
            );
        });

        it('На несуществующей организации', async function () {
            await openPage(this.browser, {size: 'm'});
            await verifyLink(
                this.browser,
                selector,
                buildLink({
                    org: YANDEX,
                    medium: 'm'
                })
            );
        });
    });
});

async function verifyLink(
    browser: WebdriverIO.Browser,
    selector: string,
    expectedHref: string | null
): Promise<void> {
    await browser.waitForVisible(selector);
    const actualHrefRaw = await browser.getAttribute(selector, 'href');
    const actualHref = Array.isArray(actualHrefRaw) ? actualHrefRaw[0] : actualHrefRaw;
    if (actualHref !== expectedHref) {
        throw new Error(`Значение атрибута "href" в "${
            selector
        }":\nОжидалось: "${expectedHref}"\nНа самом деле: "${actualHref}"`);
    }
    if (!expectedHref) {
        return;
    }
    const targetRaw = await browser.getAttribute(selector, 'target');
    const target = Array.isArray(targetRaw) ? targetRaw[0] : targetRaw;
    if (target !== '_blank') {
        throw new Error(`Ожидалось что в "${selector}" будет target="_blank", а там target="${target}"`);
    }
}

interface BuildLinkOptions {
    org?: {
        id: number;
        seoname: string;
        review?: boolean;
    };
    medium?: string;
    content?: string;
    addReview?: boolean;
}

function buildLink(
    options: BuildLinkOptions
): string {
    let baseUrl = 'https://yandex.ru/maps';
    if (options.org) {
        baseUrl += `/org/${options.org.seoname}/${options.org.id}`;
        if (options.org.review) {
            baseUrl += '/reviews';
        }
    }
    const queryOptions = ['utm_source=maps-reviews-widget'];
    if (options.medium) {
        queryOptions.push(`utm_medium=${options.medium}`);
    }
    if (options.content) {
        queryOptions.push(`utm_content=${options.content}`);
    }
    if (options.addReview) {
        queryOptions.push('add-review');
    }
    return baseUrl + `?${queryOptions.join('&')}`;
}

interface OpenPageOptions {
    orgId?: number;
    size?: Size;
    theme?: Theme;
    isOld?: boolean;
}

async function openPage(
    browser: WebdriverIO.Browser,
    options?: OpenPageOptions
): Promise<void> {
    const {
        orgId = 1124715036,
        size = 'x',
        theme = 'light',
        isOld = false
    } = options || {};
    await setViewportSize(browser, size);
    await browser.url(getLink({orgId, size, theme, isOld}));
    await browser.waitForVisible(isOld ? '.old-badge' : '.badge', 3000);
}
