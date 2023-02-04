import {getBrowserStubProxy, getHelpersProps} from './browser-stub';
import {BrowserDescriptionStub} from '../types';

function createBrowserDescriptionStub(stub: Partial<Record<string, unknown>>): BrowserDescriptionStub {
    let currentCommands: string[] = [];
    const commandsWithoutDescription: Set<string> = new Set();

    const helpersKeys = getHelpersProps(stub);
    const isStubHelper = (prop: string): boolean => {
        return (
            prop === 'then' ||
            prop === 'retrieveCommands' ||
            prop === 'getCommandsWithoutDescription' ||
            helpersKeys.includes(prop)
        );
    };
    return getBrowserStubProxy({
        browserStub: {
            ...stub,
            perform: (_fn: Function, description: string) => (description !== null ? description : ''),
            then: undefined,
            retrieveCommands: () => {
                const commands = [...currentCommands];
                currentCommands = [];

                return commands;
            },
            getCommandsWithoutDescription: () => commandsWithoutDescription
        },
        isStubHelper,
        runCommand: (command: (...args: unknown[]) => string, ...args: unknown[]): void => {
            currentCommands.push(command(...(args as never[])));
        },
        saveUnhandledCommandName: (commandName: string): void => {
            if (stub[commandName] !== null) {
                commandsWithoutDescription.add(commandName);
            }
        }
    });
}

export {createBrowserDescriptionStub};
