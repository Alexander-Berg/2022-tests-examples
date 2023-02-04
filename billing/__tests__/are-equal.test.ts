import { areEqual } from '../common';
import { DetailId, DetailProps, PersonDetailType } from '../../../types/personforms';

function getProps(props: Partial<DetailProps> = {}): Readonly<DetailProps> {
    return {
        value: '1',
        originalValue: '1',
        detail: {
            blockIndex: 1,
            id: DetailId.name,
            originalId: 'originalId',
            caption: '',
            locked: '',
            forcedLock: '',
            type: PersonDetailType.Text,
            isVisible: true,
            isDisabled: false,
            adminOnly: false,
            backofficeOnly: false,
            clientOnly: false,
            editOnly: false,
            showOnly: false
        },
        focused: undefined,
        person: {},
        isDisabled: false,
        isRequired: false,
        perms: [],
        onChange: () => {},
        ...props
    };
}

describe('common', () => {
    describe('change-person', () => {
        describe('areEqual', () => {
            test('когда меняется одно из значимых полей компонента, areEqual возвращает false (компонент не будет перерисован)', () => {
                let prev = getProps();
                let next = getProps();
                next.detail.isVisible = false;
                expect(areEqual(prev, next)).toBeFalsy();

                prev = getProps();
                next = getProps({ value: '2' });
                expect(areEqual(prev, next)).toBeFalsy();

                prev = getProps();
                next = getProps({ focused: 'name' });
                expect(areEqual(prev, next)).toBeFalsy();

                prev = getProps();
                next = getProps({ error: 'Error!' });
                expect(areEqual(prev, next)).toBeFalsy();

                prev = getProps({ error: ['Error2', 'Error1'] });
                next = getProps({ error: ['Error1', 'Error2'] });
                expect(areEqual(prev, next)).toBeFalsy();

                prev = getProps({ error: 'Error1' });
                next = getProps({ error: 'Error1' });
                expect(areEqual(prev, next)).toBeTruthy();

                prev = getProps({ error: ['Error1', 'Error2'] });
                next = getProps({ error: ['Error1', 'Error2'] });
                expect(areEqual(prev, next)).toBeTruthy();

                prev = getProps();
                next = getProps();
                expect(areEqual(prev, next)).toBeTruthy();
            });
        });
    });
});
