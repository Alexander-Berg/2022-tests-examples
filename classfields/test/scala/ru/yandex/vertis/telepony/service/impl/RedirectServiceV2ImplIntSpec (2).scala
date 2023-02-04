package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.telepony.IntegrationSpecTemplate
import ru.yandex.vertis.telepony.model.{Domain, TypedDomain, TypedDomains}
import ru.yandex.vertis.telepony.service.RedirectServiceV2Spec

/**
  * @author evans
  */
class RedirectServiceV2ImplIntSpec extends RedirectServiceV2Spec with IntegrationSpecTemplate {

  override lazy val typedDomain: TypedDomain = TypedDomains.autoru_def
  override lazy val domain: Domain = typedDomain.toString

  override def clean(): Unit = {
    try {
      super.clear()
    } finally {
      touchRedirectRequests.clear()
      clearOperatorLabelServiceInvocations(operatorLabelService)
    }

  }
}
