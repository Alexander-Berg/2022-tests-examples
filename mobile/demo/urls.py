from functools import partial

import django.conf.urls

from yaphone.advisor.demo.views import DemoProvisioningView, DemoDeviceOwnerView

demo_url = partial(django.conf.urls.url, kwargs={'application': 'demo'})

urlpatterns = [
    demo_url(r'^api/v1/provisioning/?$', DemoProvisioningView.as_view()),
    demo_url(r'^api/v1/device_owner/?$', DemoDeviceOwnerView.as_view()),
]
