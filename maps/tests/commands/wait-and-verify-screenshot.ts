import {wrapAsyncCommand} from '../lib/commands-utils';
import cssSelectors from '../common/css-selectors';
import {ScreenshotOptions} from '../types/index';

async function waitAndVerifyScreenshot(
    this: WebdriverIO.Browser,
    rawSelectors: string | string[],
    name: string,
    options: ScreenshotOptions = {}
): Promise<void> {
    if (!options.keepCursor) {
        await this.moveToObject('body', 0, 0);
    }

    if (!options.keepScroll) {
        await this.addStyles(`${cssSelectors.scrollbar} {opacity: 0;}`);
    }

    const selectors = Array.isArray(rawSelectors) ? rawSelectors : [rawSelectors];
    await Promise.all(selectors.map(async (selector) => this.waitForVisible(selector)));

    if (!options.ignoreFonts) {
        await this.waitForVisible(cssSelectors.fontsLoaded);
    }

    await this.setTimeout({script: 6000});
    const notLoadedImages = await this.executeAsync(waitForImagesLoaded);
    if (notLoadedImages) {
        throw new Error(
            'Изображения не загрузились: ' +
                notLoadedImages.map((image) => [image.className, image.url].join(' - ')).join('\n')
        );
    }

    await this.waitForLoadedAdvert();
    await this.waitForExist('.inline-image:not(._loaded)', 5000, true);
    await this.assertView(name, selectors, options);
}

/* eslint-disable */
interface LoadingImageData {
    url: string;
    className: string;
}
function waitForImagesLoaded(done: (result?: LoadingImageData[]) => void) {
    var loadingImages: LoadingImageData[] = [];

    var timeout = setTimeout(function () {
        done(loadingImages);
    }, 5000);

    function onLoad(e: Event) {
        loadingImages = loadingImages.filter(function (image) {
            return image.url !== (e.target as HTMLImageElement).src;
        });
        if (loadingImages.length === 0) {
            clearTimeout(timeout);
            done();
        }
    }

    function isElementInViewport(element: Element): boolean {
        const {top, left, right, bottom} = element.getBoundingClientRect();
        const {innerWidth, innerHeight} = window;
        return ((left >= 0 && left <= innerWidth) || (right >= 0 && right <= innerWidth)) &&
            ((top >= 0 && top <= innerHeight) || (bottom >= 0 && bottom <= innerHeight));
    }

    var allNodes = window.document.body.querySelectorAll('*');
    for (var i = 0; i < allNodes.length; i++) {
        var node = allNodes[i];
        if (node.tagName === 'IMG') {
            var image = node as HTMLImageElement;
            if (image.complete || !isElementInViewport(image)) {
                continue;
            }
            loadingImages.push({url: image.src, className: image.className});
            image.onload = onLoad;
        }

        var backgroundImage = window.getComputedStyle(node).backgroundImage;

        // tslint:disable-next-line:prefer-includes
        if (backgroundImage === 'none' || backgroundImage.indexOf('url(') !== 0) {
            continue;
        }

        var url = backgroundImage.replace('url("', '').replace('")', '');
        // tslint:disable-next-line:prefer-includes
        if (
            url.indexOf('https://') === 0 &&
            !loadingImages.some(function (image) {
                return image.url === url;
            })
        ) {
            loadingImages.push({url: url, className: node.className});

            var img = new Image();
            img.onload = onLoad;
            img.src = url;
        }
    }

    if (loadingImages.length === 0) {
        clearTimeout(timeout);
        done();
    }
}
/* eslint-enable */

export default wrapAsyncCommand(waitAndVerifyScreenshot);
