#include <yandex/maps/runtime/test-view/test_view.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/android/jni.h>
#include <yandex/maps/runtime/android/smart_ptr.h>
#include <yandex/maps/runtime/android/object.h>
#include <yandex/maps/runtime/android/cast.h>

using namespace yandex::maps::runtime;

namespace {

struct DelegateParams {
    DelegateParams(
            const view::ViewDelegateFactory& delegateFactory) :
            delegateFactory(delegateFactory)
    {
    }

    const view::ViewDelegateFactory delegateFactory;
    async::Promise<testview::ViewDelegateHandle> viewDelegatePromise;
};

} //namespace

namespace yandex::maps::runtime::testview {
ViewDelegateHandle startTestView(const view::ViewDelegateFactory& delegateFactory)
{
    auto params = std::make_unique<DelegateParams>(delegateFactory);
    auto activityStartedFuture = params->viewDelegatePromise.future();
    android::JniObject jParams = runtime::android::makeObject<DelegateParams>(std::move(params));
    async::ui()->spawn([jParams]()
    {
        auto cls = android::findClass("com/yandex/runtime/testview/TestViewActivity");
        android::callStaticMethod<void>(cls.get(), "start",
                "(Lcom/yandex/runtime/NativeObject;)V",
                jParams.get());
    }).wait();
    return activityStartedFuture.get();
}

} // namespace yandex::maps::runtime::testview
extern "C" {

JNIEXPORT void JNICALL Java_com_yandex_runtime_testview_TestViewActivity_startTestView(
        JNIEnv* env,
        jclass,
        jobject view,
        jobject jParams,
        jobject jActivity)
{
    BEGIN_NATIVE_FUNCTION
    view::PlatformView* platformView = android::object_cpp_cast<view::PlatformView>(view);

    DelegateParams* params = android::object_cpp_cast<DelegateParams>(jParams);
    android::JniObject activity(jActivity);
    auto deleter = [activity](view::ViewDelegate*)
    {
        async::ui()->spawn([activity]()
        {
            android::callMethod<void>(activity.get(), "stop", "()V");
        }).wait();

        //this is hack for waiting until async call of finalize from stop method executed
        async::ui()->spawn([]()
        {
        }).wait();
    };

    testview::ViewDelegateHandle viewHandle(platformView->createDelegate(params->delegateFactory), deleter);
    params->viewDelegatePromise.setValue(std::move(viewHandle));
    END_NATIVE_FUNCTION(env)
}

} //extern "C"
