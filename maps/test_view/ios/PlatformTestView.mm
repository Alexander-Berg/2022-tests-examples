#include <yandex/maps/runtime/test-view/test_view.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/time.h>

#include <memory>
#include <boost/any.hpp>

#import <YandexMapsMobile/YRTView_Private.h>

#import <UIKit/UIKit.h>

@interface YRTTestView : UIView

@property (nonatomic, strong) YRTView *glView;

- (id)initWithFrame:(CGRect)frame;

@end

@implementation YRTTestView

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (!self) {
        return nil;
    }

    [self addMapViewWithSize:frame.size];

    return self;
}

- (void)addMapViewWithSize:(CGSize)size
{
    _glView = [YRTViewFactory createViewWithFrame:CGRectMake(0.0, 0.0, size.width, size.height)
                                  vulkanPreferred:NO];
    [self addSubview:_glView];
}

- (void)layoutSubviews
{
    [super layoutSubviews];
    _glView.frame = self.bounds;
}

@end

namespace yandex::maps::runtime::testview {

ViewDelegateHandle startTestView(const view::ViewDelegateFactory& delegateFactory)
{
    return async::ui()->async([delegateFactory]()
    {
        UIWindow *window = [[UIApplication sharedApplication] keyWindow];
        YRTTestView *testView = [[YRTTestView alloc] initWithFrame:window.bounds];
        boost::any testViewHolder(testView);

        //testView should be deleted from ui async
        auto deleter = [testViewHolder](view::ViewDelegate*) mutable
        {
            async::ui()->spawn([](boost::any testViewHolder) mutable
            {
                @autoreleasepool {
                    YRTTestView *testView = boost::any_cast<YRTTestView *>(testViewHolder);
                    [testView removeFromSuperview];
                    testViewHolder = boost::any();
                }
            }, std::move(testViewHolder)).wait();
        };

        ViewDelegateHandle viewHandle(
                [testView.glView getPlatformView]->createDelegate(delegateFactory),
                deleter);
        [window addSubview:testView];
        return viewHandle;
    }).get();
}

} // namespace yandex::maps::runtime::testview
