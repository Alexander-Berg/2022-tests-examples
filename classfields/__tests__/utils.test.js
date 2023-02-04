import {
    createField,
    getVisibleFieldErrors,
    isFormValid,
    hideFieldError,
    changeFieldValue,
    showAllErrors
} from '../utils';

describe('validation utils', () => {
    it('should create field with default options', () => {
        const field = createField({ initialValue: '' });

        expect(field.value).toBe('');
        expect(field.showError).toBe(false);
        expect(field.validate('')).toEqual([]);
    });

    it('should filter falsy values in validator result', () => {
        const field = createField({ initialValue: '', validator: () => [ 'error', 0, '', null, false, undefined ] });

        expect(field.validate('test')).toEqual([ 'error' ]);
    });

    it('should return visible field errors', () => {
        const field = createField({ initialValue: '', showError: true, validator: v => [ ! v && 'required' ] });
        const errors = getVisibleFieldErrors(field);

        expect(errors).toEqual([ 'required' ]);
    });

    it('should not return invisible field errors', () => {
        const field = createField({ initialValue: '', validator: v => [ ! v && 'required' ] });
        const errors = getVisibleFieldErrors(field);

        expect(errors).toEqual([]);
    });

    it('should set showError to true for all fields', () => {
        const field1 = createField({ initialValue: '123', showError: false, validator: v => [ ! v && 'required' ] });
        const field2 = createField({ initialValue: '456', showError: false, validator: v => [ ! v && 'required' ] });
        const form = { field1, field2 };
        const newForm = showAllErrors(form);

        expect(newForm.field1.showError).toBe(true);
        expect(newForm.field2.showError).toBe(true);
    });

    it('should return true for valid form', () => {
        const field = createField({ initialValue: '123', validator: v => [ ! v && 'required' ] });

        expect(isFormValid({ field })).toBe(true);
    });

    it('should return false for invalid form', () => {
        const field = createField({ initialValue: '', validator: v => [ ! v && 'required' ] });

        expect(isFormValid({ field })).toBe(false);
    });

    it('should set showError to false', () => {
        const field = createField({ initialValue: '', showError: true, validator: v => [ ! v && 'required' ] });

        expect(hideFieldError('field')({ field }).field.showError).toBe(false);
    });

    it('should change field value', () => {
        const field = createField({ initialValue: '', validator: v => [ ! v && 'required' ] });

        expect(changeFieldValue('field', 'test')({ field }).field.value).toBe('test');
    });

    it('should return visible field error for field which depends on another field', () => {
        const form = {
            field1: createField({
                initialValue: 'test',
                showError: true,
                validator: (v, f) => [ (f ? f.field2.value : '' === 'some value') && v && 'required' ]
            }),
            field2: createField({
                initialValue: 'some value'
            })
        };

        const errors = getVisibleFieldErrors(form.field1, form);

        expect(errors).toEqual([ 'required' ]);
    });
});
