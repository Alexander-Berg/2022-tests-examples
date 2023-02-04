from django.conf import settings
from django.contrib.auth import get_user_model
from django.test import Client

User = get_user_model()


class WikiClient(Client):
    def login(self, login, **credentials):
        try:
            User.objects.get(username=login)
        except User.DoesNotExist:
            print(("User %s does not exist, can't login" % login))
            return False
        settings.AUTH_TEST_USER = login
        return True

    def request(self, **request):
        """Добавить HTTP_HOST если его еще нет."""
        request['HTTP_HOST'] = request.get('HTTP_HOST') or settings.API_WIKI_HOST
        # Для авторизации в Blackbox нужно указывать ip с которого пришел запрос
        request['HTTP_X_REAL_IP'] = request.get('HTTP_X_REAL_IP') or '127.0.0.1'
        return super(WikiClient, self).request(**request)

    def logout(self):
        settings.AUTH_TEST_USER = None
