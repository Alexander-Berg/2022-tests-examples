import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import Root from './Root';
import { store } from '../store';

it('renders without crashing', () => {
  const div = document.createElement('div');
  const root = (
    <Provider store={store}>
      <Root />
    </Provider>
  );
  ReactDOM.render(root, div);
});
