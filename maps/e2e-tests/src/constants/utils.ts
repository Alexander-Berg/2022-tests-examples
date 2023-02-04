export const getTlds = () => {
  let baseUrl = Cypress.config().baseUrl;

  if (baseUrl?.includes('localhost')) {
    return ['ru'];
  } else {
    return ['ru', 'com', 'tr', 'es'];
  }
};
