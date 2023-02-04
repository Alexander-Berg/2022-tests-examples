from billing.apikeys.apikeys.test_apikeys_servant import TestApiServant

servant = TestApiServant()
servant.pool = None


application = servant
