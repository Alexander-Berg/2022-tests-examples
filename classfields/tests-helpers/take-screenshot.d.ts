import { ScreenshotOptions } from 'puppeteer';

export interface ITakeScreenshotOptions extends ScreenshotOptions {
    keepCursor?: boolean;
    fullPageByClip?: boolean;
}

export default function takeScreenshot(options?: ITakeScreenshotOptions): Promise<string | Buffer>;
