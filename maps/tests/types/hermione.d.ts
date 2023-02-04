declare namespace Hermione {
    export interface TestDefinition {
        only: (expectation: string, callback?: TestDefinitionCallback) => Test;
    }

    export interface GlobalHelper {
        testPalm: {
            setComponent: (component: string) => void;
        };
        also: {
            in: (args: string | string[]) => void;
        };
    }
}
