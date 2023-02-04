from review.lib.mechanisms.base import FetchLoginAuthBackend


class Mechanism(FetchLoginAuthBackend):

    def fetch_login(self, request):
        return request.META.get('HTTP_DEBUG_LOGIN')
