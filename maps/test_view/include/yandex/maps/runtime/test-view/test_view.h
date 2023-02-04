#pragma once

#include <yandex/maps/runtime/view/platform_view.h>

#include <functional>
#include <memory>

namespace yandex::maps::runtime::testview {

typedef std::unique_ptr<view::ViewDelegate, std::function<void(view::ViewDelegate*)>> ViewDelegateHandle;
YANDEX_EXPORT ViewDelegateHandle startTestView(const view::ViewDelegateFactory& factory);

} // namespace yandex::maps::runtime::testview
