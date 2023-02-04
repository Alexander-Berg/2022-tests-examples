import {wrapAsyncCommand} from '../lib/commands-utils';

const OUTPUT_PATH = 'HERMIONE_EXTERNAL_URL_OUTPUT';

type TestWindow = typeof window & {
    [key: string]: string | undefined;
};

type ExpectedLink = string | {value: string; method: 'equal' | 'startsWith' | 'endsWith' | 'includes'};

async function waitAndVerifyLink(this: WebdriverIO.Browser, selector: string, expectedLink: ExpectedLink) {
    const {value: expectedValue, method} =
        typeof expectedLink === 'string' ? {value: expectedLink, method: 'equal' as const} : expectedLink;

    await this.waitForVisible(selector);

    const element = await this.$(selector);

    if (!element) {
        throw new Error(`Элемент ${selector} не найден`);
    }

    await this.execute(prepareLink, OUTPUT_PATH);
    await this.waitAndClick(selector);
    await this.waitUntil(
        async () => {
            const clickedLink = await this.execute(getOutputPathValue, OUTPUT_PATH);

            if (!clickedLink) {
                return false;
            }

            switch (method) {
                case 'equal':
                    if (expectedValue !== clickedLink) {
                        throw new Error(`URL ${clickedLink} не соответствует строке ${expectedValue}`);
                    }
                    break;
                case 'startsWith':
                    if (!clickedLink.startsWith(expectedValue)) {
                        throw new Error(`URL ${clickedLink} не начинается со строки ${expectedValue}`);
                    }
                    break;
                case 'endsWith':
                    if (!clickedLink.endsWith(expectedValue)) {
                        throw new Error(`URL ${clickedLink} не заканчивается строкой ${expectedValue}`);
                    }
                    break;
                case 'includes':
                    if (!clickedLink.includes(expectedValue)) {
                        throw new Error(`URL ${clickedLink} не содержит строку ${expectedValue}`);
                    }
                    break;
            }

            return true;
        },
        1000,
        `Клик в элемент ${selector} не привел к открытию ссылки`
    );
}

/* eslint-disable prefer-arrow-callback, no-undef, no-var */
function prepareLink(outputPath: string) {
    function clickListener(e: MouseEvent) {
        e.preventDefault();
        (window as TestWindow)[outputPath] = ((e.currentTarget as unknown) as HTMLHyperlinkElementUtils).href;
        listenLinkClicks('remove');
    }

    function listenLinkClicks(param: 'add' | 'remove') {
        var links = window.document.querySelectorAll('a');

        if (links) {
            for (var i = 0; i < links.length; i++) {
                if (param === 'add') {
                    links[i].addEventListener('click', clickListener);
                } else {
                    links[i].removeEventListener('click', clickListener);
                }
            }
        }
    }

    listenLinkClicks('add');

    var windowOpenBackup = window.open;

    window.open = function (externalUrl?: string) {
        window.open = windowOpenBackup;
        (window as TestWindow)[outputPath] = externalUrl;

        return null;
    };
}

function getOutputPathValue(outputPath: string) {
    return (window as TestWindow)[outputPath];
}
/* eslint-enable prefer-arrow-callback, no-undef, no-var */

export default wrapAsyncCommand(waitAndVerifyLink);
export {ExpectedLink};
