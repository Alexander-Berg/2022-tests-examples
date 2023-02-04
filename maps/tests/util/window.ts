/**
 * Opens a window (a tab) with an image to simplify verification of visualized drawings in a test.
 * In fact it opens a new tab beside the main tab (where tests are actually running) and allows to have a look at
 * what is to be saved as a reference image. And having this method call in a test updates the images
 * once the test gets rerunned.
 * Of course tests should run in a non-headless browser.
 *
 * @param imageUrl Url of an image that will be displayed, it can be a base64 encoded image data.
 */
export function openImageWindow(imageUrl: string): void {
    const w = window.open('about:blank', 'single image window')!;

    w.document.open();
    w.document.write(`<img src="${imageUrl}" style="image-rendering: pixelated; width: 100%;">`);
    w.document.close();
}
