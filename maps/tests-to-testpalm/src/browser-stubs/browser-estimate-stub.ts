import {getBrowserStubProxy, getHelpersProps} from './browser-stub';
import type {BrowserEstimateStub} from '../types';
import {minutesToMilliseconds} from '../utils';

// Коэффициент скорости работы ассессоров.
// https://st.yandex-team.ru/MAPSUI-16773#5f84734744540e3ba5b75834
const ASSESSORS_COEFFICIENT = 2.5;

function createBrowserEstimateStub(stub: Record<string, unknown>): BrowserEstimateStub {
    let currentEstimate = 0;
    const commandsWithoutEstimate: Set<string> = new Set();

    const helpersKeys = getHelpersProps(stub);
    const isStubHelper = (prop: string): boolean => {
        return (
            prop === 'then' ||
            prop === 'retrieveEstimate' ||
            prop === 'getCommandsWithoutEstimate' ||
            helpersKeys.includes(prop)
        );
    };
    return getBrowserStubProxy({
        browserStub: {
            ...stub,
            then: undefined,
            retrieveEstimate: () => {
                const estimate = currentEstimate * ASSESSORS_COEFFICIENT;
                currentEstimate = 0;

                return estimate;
            },
            getCommandsWithoutEstimate: () => commandsWithoutEstimate
        },
        isStubHelper,
        runCommand: (command: (...args: unknown[]) => number, ...args: unknown[]): void => {
            currentEstimate += minutesToMilliseconds(command(...(args as never[])));
        },
        saveUnhandledCommandName: (commandName: string): void => {
            if (stub[commandName] !== null) {
                commandsWithoutEstimate.add(commandName);
            }
        }
    });
}

export {createBrowserEstimateStub};
