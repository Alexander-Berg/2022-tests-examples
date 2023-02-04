import factory


class RouteFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'django_intranet_notifications.Route'
