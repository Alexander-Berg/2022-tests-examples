#include <yandex/maps/runtime/test-view/profiler_view_delegate.h>

#include <yandex/maps/runtime/assert.h>
#include <yandex/maps/runtime/exception.h>

#include <algorithm>
#include <cmath>

namespace yandex::maps::runtime::testview {

namespace {

const double MILLISECONDS_PER_SEC = 1000.0;

} //namespace

class Profiler {
public:
    explicit Profiler(double fpsThreshold)
        : fpsThreshold_(fpsThreshold)
        , fpsSum_(0.0)
        , squaredFpsSum_(0.0)
        , frameCount_(0)
        , minFps_(std::numeric_limits<double>::max())
        , aboveThreshold_(0) {}

    Profile profile() const
    {
        if (frameCount_ < 2) {
            return Profile();
        }

        double averageFps = fpsSum_ / (frameCount_ - 1);
        double squaredAverage = averageFps * averageFps;
        double standardDeviation = std::sqrt(squaredFpsSum_ / (frameCount_ - 1) - squaredAverage);

        return Profile(
                averageFps,
                minFps_,
                standardDeviation,
                static_cast<double>(aboveThreshold_) / static_cast<double>(frameCount_ - 1));
    }

    void handleFrame()
    {
        auto ts = now<RelativeTimestamp>();
        if (prevFrameTimestamp_) {
            uint64_t frameDuration = (ts - *(prevFrameTimestamp_)).count();
            double fps = MILLISECONDS_PER_SEC / static_cast<double>(frameDuration);
            fpsSum_ += fps;
            squaredFpsSum_ += fps * fps;
            minFps_ = std::min(fps, minFps_);

            if (fps >= fpsThreshold_) {
                aboveThreshold_++;
            }
        }

        prevFrameTimestamp_ = ts;
        frameCount_++;
    }

private:
    const double fpsThreshold_;
    boost::optional<RelativeTimestamp> prevFrameTimestamp_;
    double fpsSum_;
    double squaredFpsSum_;
    uint64_t frameCount_;
    double minFps_;
    uint64_t aboveThreshold_;
};

ProfilerViewDelegate::ProfilerViewDelegate(const std::shared_ptr<view::ViewDelegate>& viewDelegate)
    : viewDelegate_(viewDelegate)

{
    ASSERT(viewDelegate_);
}

ProfilerViewDelegate::~ProfilerViewDelegate() {}

std::shared_ptr<view::RenderState> ProfilerViewDelegate::generateRenderState()
{
    return viewDelegate_->generateRenderState();
}

void ProfilerViewDelegate::onSizeChanged() { viewDelegate_->onSizeChanged(); }

void ProfilerViewDelegate::onPaused() { viewDelegate_->onPaused(); }
void ProfilerViewDelegate::onResumed() { viewDelegate_->onResumed(); }

void ProfilerViewDelegate::onStopped() { viewDelegate_->onStopped(); }
void ProfilerViewDelegate::onStarted() { viewDelegate_->onStarted(); }

void ProfilerViewDelegate::onMemoryWarning() { viewDelegate_->onMemoryWarning(); }

bool ProfilerViewDelegate::onTouchEvent(const view::TouchEvent& event)
{
    return viewDelegate_->onTouchEvent(event);
}
bool ProfilerViewDelegate::onWheelEvent(const view::WheelEvent& event)
{
    return viewDelegate_->onWheelEvent(event);
}

void ProfilerViewDelegate::doRender(
        bool isContextLost,
        const runtime::Rect2u& viewport,
        const std::shared_ptr<view::RenderState>& renderState)
{
    std::lock_guard lock(access_);
    if (profiler_) {
        profiler_->handleFrame();
    }

    viewDelegate_->doRender(isContextLost, viewport, renderState);
}

void ProfilerViewDelegate::startProfiling(double fpsThreshold)
{
    std::lock_guard lock(access_);
    REQUIRE(!profiler_, "startProfiling called twice");
    profiler_ = std::make_unique<Profiler>(fpsThreshold);
}

Profile ProfilerViewDelegate::stopProfiling()
{
    std::lock_guard lock(access_);
    REQUIRE(profiler_, "stopProfiling called before start");
    auto profile = profiler_->profile();
    profiler_.reset();
    return profile;
}

} // namespace yandex::maps::runtime::testview
