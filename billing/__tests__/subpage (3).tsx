import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { ReactWrapper } from 'enzyme';

import { Page, PageProps } from 'common/__tests__/common.page';

import { RootLoader } from '../components/RootLoader';
import { initializeState, receivedPersons } from '../actions';

export interface PersonsSubpageProps extends PageProps {
    mockIntersectionObserver?: boolean;
}

class InteractionObserverMock {
    static callback: Function;
    constructor(callback: IntersectionObserverCallback) {
        InteractionObserverMock.callback = callback;
    }
    observe() {}
    unobserve() {}
}

export abstract class PersonsSubpage extends Page {
    intersectionObservers: [];

    constructor(props?: PersonsSubpageProps) {
        if (props?.mockIntersectionObserver) {
            //@ts-ignore
            window.IntersectionObserver = InteractionObserverMock;
        }
        super({
            ...props,
            withModules: true,
            mockWindowLocation: true,
            RootContainer: () => (
                <MemoryRouter initialEntries={['/']}>
                    <RootLoader />
                </MemoryRouter>
            )
        });
    }

    async initialize() {
        await this.sagaTester.waitFor(initializeState.type);
        this.wrapper.update();
    }

    async waitForPersons() {
        await this.sagaTester.waitFor(receivedPersons.type, true);
        this.wrapper.update();
    }

    abstract chooseArchivedPersons(): void;

    abstract getPersons(): ReactWrapper;

    hasEmptyPersons() {
        return this.getEmptyPersons().hasClass('yb-empty-persons_mode_all');
    }

    hasEmptyFilteredPersons() {
        return this.getEmptyPersons().hasClass('yb-empty-persons_mode_filtered');
    }

    emulateScrolling() {
        //@ts-ignore
        window.IntersectionObserver.callback([
            {
                isIntersecting: true
            }
        ]);
    }

    private getEmptyPersons() {
        return this.wrapper.find('.yb-empty-persons');
    }
}
