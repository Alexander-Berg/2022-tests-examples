import React from 'react';
import { DeepPartial } from 'redux';

import { Page, PageProps } from 'common/__tests__/common.page';
import { MessagesActions } from 'common/actions/messages';
import { MessagesContainer } from 'common/containers/MessagesContainer';
import { messages } from 'common/reducers/messages';
import { ContractType } from 'common/constants/print-form-rules';

import { reducers } from '../reducers';
import { rootSaga } from '../sagas';
import { Root } from '../components/Root';
import {
    ruleCardInitialized,
    ruleFormInitialized,
    showIntersectionsConfirmation,
    setAttributeReference,
    updateRule,
    addLink,
    addBlock,
    addElement,
    changeElement,
    unmarkRuleAsSaving
} from '../actions';
import { PrintFormRuleState } from '../types/state';
import { RuleAttributes, RuleElement, RuleLink } from '../types/domain';

window.React = React;

const RootContainer = () => (
    <>
        <Root />
        <MessagesContainer />
    </>
);

type PrintFormRulePageProps = PageProps & {
    initialState: DeepPartial<PrintFormRuleState>;
};

export class PrintFormRulePage extends Page {
    constructor(props: PrintFormRulePageProps) {
        super({
            ...props,
            rootSaga,
            reducers: { ...reducers, messages },
            RootContainer
        });
    }

    async waitForRuleForm() {
        await this.sagaTester.waitFor(ruleFormInitialized.type, true);
        this.wrapper.update();
    }

    async waitForRuleCard() {
        await this.sagaTester.waitFor(ruleCardInitialized.type, true);
        this.wrapper.update();
    }

    isRuleCardLoaded() {
        return this.findCardElement('title').length !== 0;
    }

    isRuleFormLoaded() {
        return this.findFormElement('title').length !== 0;
    }

    isRulePublished() {
        return this.findCardElement('status').text() === 'Опубликовано';
    }

    async validateRule() {
        this.findCardElement('validate').simulate('click');
        await this.sagaTester.waitFor(showIntersectionsConfirmation.type, true);
        this.forceRerender();
    }

    getIntersections() {
        return this.getElement('intersections-confirmation', 'intersection');
    }

    async publishRuleWithIntersections() {
        this.findCardElement('publish').simulate('click');
        await this.sagaTester.waitFor(showIntersectionsConfirmation.type, true);
        this.forceRerender();
    }

    async publishRuleWithoutIntersections() {
        this.findCardElement('publish').simulate('click');
        await this.sagaTester.waitFor(ruleCardInitialized.type, true);
        this.wrapper.update();
    }

    async confirmIntersections() {
        this.getElement('intersections-confirmation', 'accept').simulate('click');
        await this.sagaTester.waitFor(ruleCardInitialized.type, true);
        this.wrapper.update();
    }

    async goToRuleForm() {
        this.findCardElement('edit').simulate('click');
        await this.sagaTester.waitFor(ruleFormInitialized.type);
        this.wrapper.update();
    }

    async goToRuleCard() {
        this.findFormElement('cancel').simulate('click');
        await this.sagaTester.waitFor(ruleCardInitialized.type);
        this.wrapper.update();
    }

    getContractType() {
        return this.getContractTypeSelect().prop('value');
    }

    async changeContractType(contractType: ContractType) {
        (this.getContractTypeSelect().prop('onChange') as Function)(contractType);
        await this.sagaTester.waitFor(setAttributeReference.type, true);
        this.wrapper.update();
    }

    async changeContractTypeWithAcceptance(contractType: ContractType) {
        (this.getContractTypeSelect().prop('onChange') as Function)(contractType);
        await this.sagaTester.waitFor(MessagesActions.SHOW_CONFIRMATION_MESSAGE);
        this.forceRerender();
        this.getElement('messages', 'accept').simulate('click');
        await this.sagaTester.waitFor(setAttributeReference.type, true);
        this.wrapper.update();
    }

    async saveRuleWithIntersections() {
        this.findFormElement('save').simulate('click');
        await this.sagaTester.waitFor(showIntersectionsConfirmation.type, true);
        this.forceRerender();
    }

    async saveRuleWithoutIntersections() {
        this.findFormElement('save').simulate('click');
        await this.sagaTester.waitFor(ruleCardInitialized.type, true);
        this.wrapper.update();
    }

    async saveNewRule() {
        this.findFormElement('save').simulate('click');
        await this.sagaTester.waitFor(unmarkRuleAsSaving.type);
        this.wrapper.update();
    }

    async tryToSaveRule() {
        this.findFormElement('save').simulate('click');
        await this.sagaTester.waitFor(updateRule.type);
        this.wrapper.update();
    }

    getValidationErrors() {
        return this.wrapper.find('.yb-error-message');
    }

    async fillRuleAttributes(attributes: Partial<RuleAttributes>) {
        this.fillTextField('Идентификатор', attributes.externalId);
        this.fillTextField('Описание', attributes.caption);
        this.fillSelect('Тип правила', attributes.typeId);
    }

    async addRuleLink(link: Partial<RuleLink>) {
        this.getElement('rule-links', 'add-rule-link').simulate('click');
        await this.sagaTester.waitFor(addLink.type);
        this.getElement('rule-link', 'name', 'input').simulate('change', {
            target: { value: link.name }
        });
        this.getElement('rule-link', 'value', 'input').simulate('change', {
            target: { value: link.value }
        });
    }

    async addRuleBlock() {
        this.getElement('rule-blocks', 'add-rule-block').simulate('click');
        await this.sagaTester.waitFor(addBlock.type);
    }

    async addRuleElement(element: Partial<RuleElement>) {
        this.getElement('rule-elements', 'add-rule-element').simulate('click');
        await this.sagaTester.waitFor(addElement.type);
        this.wrapper.update();

        await this.fillRuleElement('context-id', 'Select', element.contextId);
        await this.fillRuleElement('attribute-id', 'Select', element.attributeId);
        await this.fillRuleElement(
            'attribute-comparision-type',
            'Select',
            element.attributeComparisionType
        );
        await this.fillRuleElement('value', 'ComboBox', element.value);
    }

    private getContractTypeSelect() {
        return this.getComponent('rule-attributes', 'contract-type', 'Select');
    }

    private async fillRuleElement(element: string, selector: string, value?: string | number) {
        const select = this.getComponent('rule-element', element, selector);
        (select.prop('onChange') as Function)(value);
        await this.sagaTester.waitFor(changeElement.type);
        this.wrapper.update();
    }

    private findCardElement(element: string, selector?: string) {
        return this.getElement('rule-card', element, selector);
    }

    private findFormElement(element: string, selector?: string) {
        return this.getElement('rule-form', element, selector);
    }

    private getElement(block: string, element: string, selector?: string) {
        return this.wrapper
            .find(`.yb-${block}__${element}${selector ? ` ${selector}` : ''}`)
            .hostNodes();
    }

    private getComponent(block: string, element: string, component: string) {
        return this.wrapper.find(`.yb-${block}__${element}`).find(component);
    }
}
