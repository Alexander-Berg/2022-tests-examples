import {getBrowserBaseStub} from './browser-base-stub';
import {GetBrowserStub} from '../types';

const parseSelector = (str?: string): string => (str ? '`' + str + '`' : '');

const getBrowserDescriptionStub: GetBrowserStub<string> = (platform) => ({
    ...getBrowserBaseStub(platform),
    goto: (url: string) => `Открыть ${url}`,
    waitForSelector: (selector: string) => `Дождаться отображения ${parseSelector(selector)}`,
    click: (selector: string) => `Кликнуть в элемент ${parseSelector(selector)}`
});

export {getBrowserDescriptionStub};
