require('jest-enzyme');

const React = require('react');
const Enzyme = require('enzyme');
const Adapter = require('@wojtekmaj/enzyme-adapter-react-17');
const { createSerializer } = require('enzyme-to-json');

Enzyme.configure({ adapter: new Adapter() });

expect.addSnapshotSerializer(createSerializer({ mode: 'deep' }));

global.React = React;
