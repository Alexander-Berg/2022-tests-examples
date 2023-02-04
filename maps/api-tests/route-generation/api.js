const fetch = require('node-fetch');

const config = require('../config');

const fetchApi = async (method, url, data, headers = {}) => {
  const requestHeaders = {
    Authorization: `Auth ${config.superUserToken}`,
    'Content-type': 'application/json',
    ...headers,
  };
  const requestUrl = `${config.endpoint}/${url}`;

  //console.log(`${method} ${url}`);
  const body = data && JSON.stringify(data);

  const response = await fetch(requestUrl, { headers: requestHeaders, method, body });
  const responseBody = await response.json();
  if (response.status !== 200) {
    throw new Error(
      `Request error. Url: ${requestUrl}. Status: ${response.status}. Response: ${JSON.stringify(
        responseBody,
      )}`,
    );
  }

  return responseBody;
};

module.exports = fetchApi;
