module.exports = {
  appUserToken: process.env.APP_USER_TOKEN || null,
  superUserToken: process.env.SUPER_USER_TOKEN || null,
  nonexistentToken: 'NONEXISTENT_TOKEN',
  unregisteredToken: process.env.UNREGISTERED_USER_TOKEN || null,
  testData: {},
};
