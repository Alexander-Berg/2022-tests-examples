import {getBrowserStubProxy, getHelpersProps} from './browser-stub';
import type {BrowserPreconditionsStub} from '../types';

type PreconditionsData = Record<string, string>;

function createBrowserPreconditionsStub(stub: Record<string, unknown> & {template: string}): BrowserPreconditionsStub {
    let currentPreconditionsData: PreconditionsData = {};

    const helpersKeys = getHelpersProps(stub);
    const isStubHelper = (prop: string): boolean => {
        return prop === 'then' || prop === 'retrievePreconditionsData' || helpersKeys.includes(prop);
    };
    return getBrowserStubProxy({
        browserStub: {
            ...stub,
            then: undefined,
            retrievePreconditionsData: () => {
                const preconditionsData = {
                    ...currentPreconditionsData
                };
                currentPreconditionsData = {};

                return getPreconditions(stub.template, preconditionsData);
            }
        },
        isStubHelper,
        runCommand: (command: (...args: unknown[]) => PreconditionsData, ...args: unknown[]): void => {
            currentPreconditionsData = {
                ...currentPreconditionsData,
                ...command(...(args as never[]))
            };
        }
    });
}

function getPreconditions(template: string, preconditionsData: Partial<PreconditionsData>): string {
    return Object.entries(preconditionsData).reduce((template, [key, value]) => {
        if (!value) {
            return template;
        }

        const regexp = new RegExp(`{{${key}}}`, 'g');
        return template.replace(regexp, value);
    }, template);
}

export {createBrowserPreconditionsStub};
