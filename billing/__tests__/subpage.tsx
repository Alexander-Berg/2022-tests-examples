import React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { Page, PageProps } from 'common/__tests__/common.page';
import { ChangePersonAction } from 'common/modules/change-person/actions';
import { messages } from 'common/reducers/messages';
import { MessagesActions } from 'common/actions/messages';
import { MessagesContainer } from 'common/containers/MessagesContainer';

import { EditPersonRoot, NewPersonRoot } from '../components/RootLoaders';
import { changePersonCategory, initialized } from '../actions';

interface ChangePersonSubpageProps extends PageProps {
    personId?: string;
}

export class ChangePersonSubpage extends Page {
    constructor(props?: ChangePersonSubpageProps) {
        super({
            ...props,
            withModules: true,
            reducers: {
                messages
            },
            RootContainer: () => (
                <MemoryRouter initialEntries={['/']}>
                    <MessagesContainer />
                    {props?.personId ? (
                        <EditPersonRoot
                            match={{ params: { personId: props?.personId } }}
                            location={{}}
                        />
                    ) : (
                        <NewPersonRoot match={{ params: {} }} location={{}} />
                    )}
                </MemoryRouter>
            )
        });
    }

    async initialize() {
        await this.sagaTester.waitFor(initialized.type);
        this.wrapper.update();
    }

    isInitialized() {
        return this.getPersonCategoryButtons().length > 0;
    }

    async choosePersonCategory(category: string) {
        (this.getPersonCategorySelect().prop('onChange') as Function)({ id: category });

        await this.sagaTester.waitFor(changePersonCategory.type);
        this.wrapper.update();
    }

    async continueWithPersonCategory() {
        this.getPersonCategoryButtons().find('button').simulate('click');
        this.wrapper.update();
    }

    async changePersonCategory(category: string) {
        this.choosePersonCategory(category);
        await this.sagaTester.waitFor(MessagesActions.SHOW_CONFIRMATION_MESSAGE);
        this.wrapper.update();
    }

    async waitForChangePersonForm() {
        await this.sagaTester.waitFor(ChangePersonAction.LOADED);
        this.wrapper.update();
    }

    async saveChangePersonForm() {
        this.wrapper.find('form').simulate('submit');
        await this.sagaTester.waitFor(ChangePersonAction.RECEIVE_CHANGE_SUCCESS);
        await this.nextTick();
        this.wrapper.update();
    }

    async waitForErrorMessage() {
        await this.sagaTester.waitFor(MessagesActions.SHOW_ERROR_MESSAGE);
        this.wrapper.update();
    }

    isChangePersonFormLoaded() {
        return this.wrapper.find('form .buttons').length > 0;
    }

    getMessageText() {
        return this.wrapper.find('.yb-new-persons-messages').text();
    }

    getMessageBoxText() {
        return this.wrapper.find('.yb-messages__text').text();
    }

    isCategorySelectDisabled() {
        return this.getPersonCategorySelect().prop('disabled');
    }

    private getPersonCategorySelect() {
        return this.wrapper.find(`Field[name="Тип плательщика"]`).find('Select');
    }

    private getPersonCategoryButtons() {
        return this.wrapper.find('.yb-change-person-category-field__buttons');
    }
}
