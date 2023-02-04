import {URL} from 'url';
import chai, {expect} from 'chai';
import {getSeoTestFiles} from './utils';
import {getUrl, getHost} from '../../lib/func/url';
import {SeoCase} from './types';

const isCI = Boolean(process.env.CI);

chai.config.showDiff = true;
chai.config.truncateThreshold = 0;

function replaceStandToCanonical(stringUrl: string): string {
    return stringUrl.replace(/(https?:\/\/).*(yandex)/, '$1$2');
}

const canonicalHostname = getHost().replace(/https?:\/\/(.*)yandex.*/, '$1');

function replaceCanonicalToStand(url: string): string {
    return url.replace(/(https?:\/\/).*(yandex)/, `$1${canonicalHostname}$2`);
}

describe('SEO', () => {
    getSeoTestFiles().forEach((fileContent) => {
        if (Array.isArray(fileContent)) {
            fileContent.forEach(run);
        } else if (typeof fileContent === 'object' && 'specs' in fileContent) {
            const isExclusive = fileContent.only;
            const method = isExclusive ? describe.only : describe;
            if (fileContent.skip) {
                hermione.skip.in(/.*/, fileContent.skip);
            }
            method(fileContent.name, () => {
                if (isExclusive && isCI) {
                    throw new Error(`Exclusive test in CI: ${fileContent.name}`);
                }
                fileContent.specs.forEach(run);
            });
        } else {
            run(fileContent);
        }
    });
});

// Один тесткейс = один лендинг.
function run(testCase: SeoCase): void {
    if (testCase.skip) {
        hermione.skip.in(/.*/, testCase.skip);
    }
    const isExclusive = testCase.only;
    const method = isExclusive ? describe.only : describe;
    if (isExclusive && isCI) {
        throw new Error(`Exclusive test in CI: ${testCase.name}`);
    }

    const urlOptions = {
        disableJavaScript: true,
        tld: testCase.tld || 'ru',
        mockVersion: testCase.mockVersion
    };

    if (testCase.check404) {
        method(testCase.name, () => {
            it('Отдает роботу 404', async function () {
                const url = getUrl(testCase.url, urlOptions);

                await this.browser.url(url);
                const currentBrowserUrl = await this.browser.getUrl();
                if (currentBrowserUrl.includes('stand')) {
                    const content = await this.browser.getText('body > pre:only-child');
                    expect(content).to.be.equal('Not Found');
                } else {
                    const content = await this.browser.getText('h1.content__title');
                    expect(content).to.be.include('404');
                }
            });
        });

        return;
    }

    method(testCase.name, () => {
        beforeEach(async function () {
            const url = getUrl(testCase.url, urlOptions);
            await this.browser.setTimeout({pageLoad: 10000});
            await this.browser.url(url);
            await this.browser.checkStandError();
        });

        const og = testCase.og;
        const alternates = testCase.alternates;
        if (og) {
            describe('OpenGraph разметка', () => {
                Object.entries(og).forEach(([ogName, ogValue]) => {
                    it(`og:${ogName}`, async function () {
                        const content = await this.browser.getAttribute(`meta[property="og:${ogName}"]`, 'content');
                        if (ogName === 'url') {
                            expect(replaceStandToCanonical(content)).to.be.equal(ogValue);
                        } else {
                            expect(decodeURIComponent(content)).to.be.equal(ogValue);
                        }
                    });
                });
            });
        }

        if (testCase.breadcrumbList) {
            it('Хлебные крошки', async function () {
                await this.browser.verifyLdJson('BreadcrumbList', {
                    '@context': 'https://schema.org',
                    '@type': 'BreadcrumbList',
                    itemListElement: testCase.breadcrumbList!.map((breadcrumb, index) => ({
                        '@type': 'ListItem',
                        position: index + 1,
                        item: {
                            '@id': replaceCanonicalToStand(breadcrumb.url),
                            name: breadcrumb.name
                        }
                    }))
                });
            });
        }

        if (testCase.title || testCase.description || testCase.h1) {
            it('title/description/h1', async function () {
                if (testCase.title) {
                    await this.browser.verifyTitle(testCase.title);
                }
                if (testCase.description) {
                    const description = await this.browser.getAttribute('meta[name=description]', 'content');
                    expect(description).to.be.equal(testCase.description, 'Description не совпадает');
                }
                if (testCase.h1) {
                    await this.browser.waitAndCheckValue('h1', testCase.h1);
                }
            });
        }

        if (testCase.canonical) {
            it('Канонический адрес', async function () {
                const href = await this.browser.getAttribute('link[rel=canonical]', 'href');
                expect(replaceStandToCanonical(decodeURI(href))).to.be.equal(testCase.canonical);
            });
        }

        if (alternates) {
            it('Альтернативы', async function () {
                const pageAlternateCount = (await this.browser.$$('link[rel=alternate]')).length;
                if (pageAlternateCount !== alternates.length) {
                    throw new Error('Количество альтернатив на странице не равно количеству в тесте');
                }

                for (const {href, hreflang} of alternates) {
                    const alternateElement = await this.browser.$(
                        `link[rel=alternate][href="${replaceCanonicalToStand(
                            encodeURI(href)
                        )}"][hreflang="${hreflang}"]`
                    );

                    if (alternateElement.error) {
                        throw new Error(`Alternate with href "${href}" and hreflang "${hreflang}" not found`);
                    }
                }
            });
        }

        if (testCase.noIndex !== undefined) {
            it('Индексация страницы', async function () {
                const noIndex = (await this.browser.$$('meta[name="robots"][content="noindex, nofollow"]')).length > 0;
                expect(testCase.noIndex).to.be.equal(noIndex);
            });
        }

        if (testCase.canonicalBrowserPath) {
            it('Приведение пути к каноническому виду в браузере', async function () {
                await this.browser.waitForUrlContains({path: testCase.canonicalBrowserPath}, {skipUrlControlled: true});
            });
        }

        const meta = testCase.schemaVerifications;
        if (meta) {
            it('Валидация специфичных HTML селекторов', async function () {
                for (const metaItem of meta) {
                    const value = 'value' in metaItem ? metaItem.value : undefined;
                    const content = 'content' in metaItem ? metaItem.content : undefined;
                    const amount =
                        'amount' in metaItem
                            ? metaItem.amount
                            : value
                            ? Array.isArray(value)
                                ? value.length
                                : 1
                            : Array.isArray(content)
                            ? content.length
                            : 1;
                    await this.browser.waitForElementsCount(metaItem.selector, amount);
                    if (value) {
                        await this.browser.waitAndCheckValue(metaItem.selector, value);
                    }
                    if (content) {
                        await this.browser.verifyAttribute(metaItem.selector, 'content', content);
                    }
                }
            });
        }

        const jsonLd = testCase.jsonLd;
        if (jsonLd) {
            it(`Разметка ${jsonLd.type}`, async function () {
                await this.browser.verifyLdJson(jsonLd.type, jsonLd.value);
            });
        }

        if (testCase.redirectUrl) {
            it('Редирект', async function () {
                const url = getUrl(testCase.url, urlOptions);
                await this.browser.url(url);
                await this.browser.checkStandError();
                await this.browser.waitForUrlContains(
                    {path: testCase.redirectUrl},
                    {partial: true, skipUrlControlled: true}
                );
            });
        } else {
            it('Нет редиректа', async function () {
                const url = getUrl(testCase.url, urlOptions);
                await this.browser.url(url);
                await this.browser.checkStandError();
                await this.browser.waitForUrlContains(
                    {path: new URL(url).pathname},
                    {partial: true, keepBaseUrl: true, skipUrlControlled: true}
                );
            });
        }
    });
}
