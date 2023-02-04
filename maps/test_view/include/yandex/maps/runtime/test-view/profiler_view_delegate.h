#pragma once

#include <yandex/maps/runtime/view/view_delegate.h>
#include <yandex/maps/runtime/time.h>

#include <boost/optional.hpp>
#include <cstdint>
#include <limits>
#include <memory>
#include <mutex>

namespace yandex::maps::runtime::testview {

struct YANDEX_EXPORT Profile {
    Profile() : averageFps(0.0), minFps(0.0), standardDeviation(0.0), aboveThreshold(0.0) {}
    Profile(double averageFps, double minFps, double standardDeviation, double aboveThreshold)
        : averageFps(averageFps)
        , minFps(minFps)
        , standardDeviation(standardDeviation)
        , aboveThreshold(aboveThreshold) {}

    double averageFps;
    double minFps;
    double standardDeviation;
    double aboveThreshold;
};

class Profiler;
class YANDEX_EXPORT ProfilerViewDelegate : public view::ViewDelegate {
public:
    explicit ProfilerViewDelegate(const std::shared_ptr<view::ViewDelegate>& viewDelegate);
    virtual ~ProfilerViewDelegate();

    //ViewDelegate
    virtual std::shared_ptr<view::RenderState> generateRenderState() override;
    virtual void onSizeChanged() override;
    virtual void onPaused() override;
    virtual void onResumed() override;
    virtual void onStopped() override;
    virtual void onStarted() override;
    virtual void onMemoryWarning() override;
    virtual bool onTouchEvent(const view::TouchEvent& event) override;
    virtual bool onWheelEvent(const view::WheelEvent& event) override;
    virtual void doRender(
        bool isContextLost,
        const runtime::Rect2u& viewport,
        const std::shared_ptr<view::RenderState>& renderState) override;

    view::ViewDelegate* impl() { return viewDelegate_.get(); }

    //profiler will calculate how many frames were rendered faster than fpsThreshold
    void startProfiling(double fpsThreshold);
    Profile stopProfiling();

private:
    std::shared_ptr<view::ViewDelegate> viewDelegate_;
    std::mutex access_;
    std::unique_ptr<Profiler> profiler_;
};

} // namespace yandex::maps::runtime::testview
