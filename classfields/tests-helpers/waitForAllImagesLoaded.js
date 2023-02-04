/* global page */

async function waitForAllImagesLoaded() {
    // eslint-disable-next-line no-undef
    const definedDocument = document;

    const selectors = Array.from(definedDocument.querySelectorAll('img'));

    await Promise.all(
        selectors.map(img => {
            if (img.complete) {
                return;
            }
            return new Promise((resolve, reject) => {
                img.addEventListener('load', resolve);
                img.addEventListener('error', reject);
            });
        })
    );

    // Ждём загрузки всех изображений, указанных через backgroung-image
    const allElements = Array.from(definedDocument.querySelectorAll('*'));
    const backgroundImagesUrls = allElements.reduce((total, next) => {
        const backgroundImageValue = definedDocument?.defaultView
            ?.getComputedStyle(next)
            .getPropertyValue('background-image');

        if (! backgroundImageValue || backgroundImageValue === 'none') {
            return total;
        }

        return total.concat(backgroundImageValue.replace(/^url\(["']?/, '').replace(/["']?\)$/, ''));
    }, []);

    await Promise.allSettled(
        backgroundImagesUrls.map(imageUrl => {
            // eslint-disable-next-line no-undef
            const img = new Image();

            const promise = new Promise((resolve, reject) => {
                img.addEventListener('load', resolve);
                img.addEventListener('error', reject);
            });

            img.src = imageUrl;

            return promise;
        })
    );
}

module.exports.waitForAllImagesLoaded = waitForAllImagesLoaded;
