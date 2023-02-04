import focusField from './actions/focusField';
import reducer from './reducer';

it('должен запомнить переданный focusField', () => {
    let state = reducer(undefined, focusField('field'));
    expect(state).toEqual('field');

    state = reducer(undefined, focusField(''));
    expect(state).toEqual('');
});
