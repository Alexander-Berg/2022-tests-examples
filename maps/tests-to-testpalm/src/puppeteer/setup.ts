import {Target, Page as PuppeteerPage, Browser} from 'puppeteer';
import type {SetComponentCommand} from '../types';
import type {PuppeteerPerformCommand} from '../types/puppeteer';

interface Page extends PuppeteerPage {
    perform: PuppeteerPerformCommand;
    setComponent: SetComponentCommand;
}

declare const page: Page;
declare const browser: Browser;

declare global {
    // @ts-ignore
    const page: Page;
}

browser.on('targetcreated', async (target: Target) => {
    if (target.type() === 'page') {
        const page = await target.page() as Page | null;
        if (page) {
            page.perform = (callback) => Promise.resolve(callback());
            page.setComponent = () => {};
        }
    }
});

export {};
