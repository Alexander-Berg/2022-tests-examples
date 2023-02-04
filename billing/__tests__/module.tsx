import React from 'react';

import { Page, PageProps } from 'common/__tests__/common.page';

import { DynamicChangePerson } from '../components/DynamicChangePerson';
import { ChangePersonAction } from '../actions';
import { DynamicChangePersonParams } from '../types';

interface ChangePersonModuleProps extends PageProps {
    clientId: string;
    personType?: string;
    personId?: string;
    onSave?: DynamicChangePersonParams['onSave'];
    onCancel?: DynamicChangePersonParams['onCancel'];
}

export class ChangePersonModule extends Page {
    constructor(props?: ChangePersonModuleProps) {
        super({
            withModules: true,
            mocks: props?.mocks,
            RootContainer: () => (
                <DynamicChangePerson
                    clientId={props?.clientId}
                    personType={props?.personType}
                    personId={props?.personId}
                    onSave={props?.onSave}
                    onCancel={props?.onCancel}
                    isPartner
                />
            )
        });
    }

    async initializeModule() {
        await this.sagaTester.waitFor(ChangePersonAction.LOADED);
        this.wrapper.update();
    }

    async saveForm() {
        this.wrapper.simulate('submit');
        await this.sagaTester.waitFor(ChangePersonAction.RECEIVE_CHANGE_SUCCESS);
        await this.nextTick();
        this.wrapper.update();
    }

    async cancelForm() {
        this.getFormButtons().find('a').simulate('click');
        await this.sagaTester.waitFor(ChangePersonAction.CANCEL);
        await this.nextTick();
        this.wrapper.update();
    }

    isFormLoaded() {
        return this.getFormButtons().length > 0;
    }

    private getFormButtons() {
        return this.wrapper.find('section.buttons');
    }
}
