package ru.yandex.vertis.general.common.model.editor.testkit

import general.common.editor_model.{AutomaticEditor, ModeratorEditor, SellerEditor}
import general.common.platform_model.PlatformEnum
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.common.model.user.SellerId.toApiSellerId

object Editors {

  def seller(sellerId: SellerId) =
    SellerEditor(Some(toApiSellerId(sellerId)), "127.0.0.1", PlatformEnum.Platform.ANDROID_TURBO)

  def moderator(id: String) = ModeratorEditor(id, "127.0.0.2")

  def automatic(id: String) = AutomaticEditor(id)
}
