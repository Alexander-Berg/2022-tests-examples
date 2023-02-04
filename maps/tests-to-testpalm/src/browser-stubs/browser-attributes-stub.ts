import {TestCaseAttributes} from '@yandex-int/testpalm-api';
import {getBrowserStubProxy, getHelpersProps} from './browser-stub';
import type {BrowserAttributesStub} from '../types';

function createBrowserAttributesStub(stub: Record<string, unknown>): BrowserAttributesStub {
    let currentAttributes: TestCaseAttributes = {};
    const helpersKeys = getHelpersProps(stub);
    return getBrowserStubProxy({
        browserStub: {
            ...stub,
            then: undefined,
            retrieveAttributes: () => {
                const attributes = {...currentAttributes};
                currentAttributes = {};

                return attributes;
            }
        },
        isStubHelper: (prop: string): boolean => {
            return prop === 'then' || prop === 'retrieveAttributes' || helpersKeys.includes(prop);
        },
        runCommand: (command: () => TestCaseAttributes): void => {
            currentAttributes = {
                ...currentAttributes,
                ...command()
            };
        }
    });
}

export {createBrowserAttributesStub};
