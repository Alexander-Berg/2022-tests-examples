/* eslint-disable @typescript-eslint/no-explicit-any */
// eslint-disable-next-line spaced-comment
/// <reference types='hermione' />

declare namespace WebdriverIO {
    type ElementId = string;

    interface Size {
        width: number;
        height: number;
    }

    interface Position {
        x: number;
        y: number;
    }

    type Axis = 'x' | 'y';

    export interface ExecutionContext {
        browserId: string;
        meta: Record<string, unknown>;
    }

    interface LogEntry {
        timestamp: number;
        level: string;
        message: string;
    }

    interface Browser {
        isPhone: boolean;
        executionContext: ExecutionContext;

        log(type: string): Promise<LogEntry[]>;

        click(selector?: string): Promise<null>;
        doubleClick(selector?: string): Promise<null>;
        dragAndDrop(sourceElem: string, destinationElem: string): Promise<null>;
        middleClick(selector?: string): Promise<null>;
        middleClick(selector: string, xoffset?: number, yoffset?: number): Promise<null>;
        moveToObject(selector?: string): Promise<null>;
        moveToObject(selector: string, xoffset?: number, yoffset?: number): Promise<null>;
        rightClick(selector?: string): Promise<null>;
        rightClick(selector: string, xoffset?: number, yoffset?: number): Promise<null>;

        clearElement(selector?: string): Promise<null>;
        addValue(selector: string, value: string | number): Promise<null>;
        setValue(selector: string, values: number | string | string[]): Promise<null>;
        getValue(selector?: string): Promise<string>;
        getValue(selector?: string): Promise<string[]>;

        getText(selector?: string): Promise<string>;
        getText(selector?: string): Promise<string[]>;

        getAttribute(selector: string, attributeName: string): Promise<string> & null;
        getAttribute(selector: string, attributeName: string): Promise<string[] & null[]>;
        getAttribute(attributeName: string): Promise<string> & null;
        getAttribute(attributeName: string): Promise<string[] & null[]>;
        getAttribute<P extends string | string[] | null | null[]>(selector: string, attributeName: string): Promise<P>;

        frame(id: any): Promise<null>;
        refresh(): Promise<null>;
        url(): Promise<string>;
        url(url: string): Promise<null>;

        isVisible(selector?: string): Promise<boolean>;

        waitUntil(
            condition: () => Promise<boolean> | any,
            timeout?: number,
            timeoutMsg?: string,
            interval?: number
        ): Promise<boolean>;

        getViewportSize(): Promise<Size>;
        setViewportSize(size: Size, type?: boolean): Promise<undefined>;
    }
}
