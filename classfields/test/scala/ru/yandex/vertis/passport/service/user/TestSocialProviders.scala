package ru.yandex.vertis.passport.service.user

import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.model.SocialProviders

/**
  *
  * @author zvez
  */
trait TestSocialProviders {

  val socialProviders = Seq(
    new DummySocialProviderServiceYandex(SocialProviders.Hsd, true),
    new DummySocialProviderServiceYandex(SocialProviders.VK, false)
  )
}

trait SpiedSocialProviders extends MockitoSupport {
  val socialProviderHsd = Mockito.spy(new DummySocialProviderServiceYandex(SocialProviders.Hsd, true))
  val socialProviderVk = Mockito.spy(new DummySocialProviderServiceYandex(SocialProviders.VK, false))

  val socialProviders = Seq(
    socialProviderHsd,
    socialProviderVk
  )
}
