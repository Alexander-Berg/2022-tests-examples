#include <yandex/maps/runtime/test-view/test_view.h>

#include <yandex/maps/runtime/view/qt/glwidget.h>
#include <yandex/maps/runtime/async/promise.h>

#include <memory>

namespace yandex::maps::runtime::testview {

namespace {

class TestView : public runtime::view::qt::GLWidget {
public:
    TestView()
    {
        resize(sizeHint());
    }

    view::PlatformView* createPlatformView() { return getPlatformView(); }
};

} //namespace

ViewDelegateHandle startTestView(const view::ViewDelegateFactory& delegateFactory)
{
    return async::ui()->async([delegateFactory]()
    {
        auto testView = new TestView();
        auto deleter = [testView](view::ViewDelegate*)
        {
            async::ui()->spawn([testView]()
            {
                testView->hide();
                delete testView;
            }).wait();
        };
        ViewDelegateHandle viewHandle(
                testView->createPlatformView()->createDelegate(delegateFactory), deleter);
        testView->show();
        return viewHandle;
    }).get();
}

} // namespace yandex::maps::runtime::testview
