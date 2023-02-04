/* eslint-disable regexp/no-useless-escape */
const React = require('react');
const { render } = require('@vertis/jest-puppeteer-react');
const { kebabCase } = require('lodash');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMockBrowser');
const Context = createContextProvider(contextMock);
const CyrillicToTranslit = require('cyrillic-to-translit-js');

const cyrillicToTranslit = new CyrillicToTranslit();

require('storybook/components/index.css');

// В значениях пропов могут встречаться всякие символы, которые не хотим видеть в имени файла, но которые нужны для уникальности этого имени
// eslint-disable-next-line no-useless-escape
const specialCharacterRegexp = /[#\?]/g;
const specialCharacterMap = {
    '#': 'sharp',
    '?': 'question',
};

function testStory(Component) {
    const componentName = Component.displayName || Component.name;

    it(`${ componentName } screenshot test`, async() => {
        let cases;

        const page = await render(
            <Context>
                <Component/>
            </Context>,
            {
                // FIXME
                //  сейчас мы руками задаем высоту вьюпорта больше высоты контента страницы, чтобы все элементы были видны
                //  по идее element.screenshot должен избавлять нас от этой проблемы
                //  согласно доке он scrolls element into view if needed
                //  но он почему-то флапает - часть кейсов получаются пустыми, часть нормальными
                //  я пока не разобрался почему так происходит
                viewport: { width: 840, height: 4000 },
                defaultViewport: null,
                after: async(page) => {
                    cases = await page.evaluate(() => {
                        return Array.from(document.querySelectorAll(`[data-test='1']`), (element) => {
                            return [ element.dataset.propName, element.dataset.propValue ];
                        });
                    });
                },
            },
        );

        await Promise.all(cases.map(async([ prop, value ]) => {
            const selector = [
                `[data-test='1']`,
                // в StoryModule нет prop
                prop ? `[data-prop-name='${ prop }']` : '',
                // но value должно быть всегда
                `[data-prop-value='${ value }']`,
            ].filter(Boolean).join('');
            const testMarker = await page.$(selector);
            const element = await testMarker.evaluateHandle((node) => node.nextSibling);
            const rect = await element.evaluate((node) => {
                const { height, width, x, y } = node.getBoundingClientRect();
                return { x, y, width, height };
            });
            const padding = 10;
            const screenshot = await element.screenshot({
                clip: {
                    x: rect.x - padding,
                    y: rect.y - padding,
                    width: rect.width + padding * 2,
                    height: rect.height + padding * 2,
                },
            });

            const identifier = [
                `${ componentName }__`,
                prop ? `${ prop }=` : '',
                value,
            ].filter(Boolean).join('').replace(specialCharacterRegexp, (match) => specialCharacterMap[match] || match.charCodeAt(0));

            const testName = cyrillicToTranslit.transform(kebabCase(identifier));

            expect(screenshot).toMatchImageSnapshot({
                customSnapshotIdentifier: () => testName,
            });
        }));

    });
}

// eslint-disable-next-line jest/no-export
module.exports = testStory;
