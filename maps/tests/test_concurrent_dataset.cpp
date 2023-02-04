#include <maps/factory/libs/dataset/concurrent_dataset.h>

#include <maps/factory/libs/image/image.h>

#include <maps/factory/libs/common/eigen.h>
#include <maps/factory/libs/unittest/tests_common.h>

#include <maps/libs/log8/include/log8.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;

namespace {

// These tiffs have overviews.
const std::string MUL_PATH =
    ArcadiaSourceRoot() + "/maps/factory/test_data/cog/scanex_13174627/MUL.TIF";
const std::string IKONOS_BAND_PATH =
    ArcadiaSourceRoot() + "/maps/factory/test_data/geotif_corpus/ikonos_3395_inter_band.tif";

template <typename T>
class TestDataset final : public ConcurrentDataset<T> {
public:
    TestDataset(
        const std::string& path, BlockCachePtr bc,
        int ovr = ConcurrentDataset<T>::BASE_DATASET)
        : ConcurrentDataset<T>(bc, ovr)
        , path_(path)
    {
        this->setName(path);

        TDataset ds = OpenDataset(path_);

        const Size2i size = ds.size();
        const Index n = ds.overviewsCount();

        if (this->isBaseDataset()) {
            DEBUG() << "Ovr count " << n << " " << path;
            this->setSize(size);
            this->setGeoTransform(ds.Site().PixToProj());
        } else {
            const Size2i ovrSize = overviewSize(size, ovr - 1);
            ratio_ = size.cast<double>() / ovrSize.cast<double>();
            this->setSize(ovrSize);
            this->setGeoTransform(setPixelSize(ds.Site().PixToProj(), ds.Site().PixelSize().array() * ratio_));
        }

        this->setProjection(ds.Site().Sr());
        this->addBands(ds.Dim().Bands());

        if (this->isBaseDataset()) {
            for (Index i = 1; i <= n; ++i) {
                this->addOverviewDataset(Shared<TestDataset>(path, bc, i));
            }
        }
    }

private:
    void loadBlock(TBlock& block, const Array2i& offset) override
    {
        const Box2i box = this->blockBox(offset);
        auto readImg = block.Image<T>(box.sizes());
        const Box2d readBox = box.cast<double>() * ratio_;
        TDataset ds = OpenDataset(path_);
        ds.Read(readImg, readBox);
        auto img = block.Image<T>();
        img.extendInplace(box.sizes());
    }

    Array2d ratio_{1, 1};
    std::string path_;
};

template <typename T>
void checkReadAll(
    const std::string& path, double scale,
    ResampleAlg alg = ResampleAlg::NearestNeighbour
)
{
    auto bc = std::make_shared<TLruBlockCache>();
    TDataset ds = OpenDataset(path);
    const Size2d size = ds.Dim().Size().cast<double>();
    const Size2i imgSize = (size * scale).cast<int>();
    const Index bands = ds.Dim().Bands();
    Box2d box(Point2d::Zero(), size);

    TestDataset<T> cDs(path, bc);
    Image<T> cImg(imgSize, bands);
    cDs.ds().Read(cImg, box, alg);

    Image<T> img(imgSize, bands);
    ds.Read(img, box, alg);

    EXPECT_EQ(cImg.maxAbsDifference(img), 0) << path << "; "
                    << scale << "; " << alg;
}

template <typename T>
void checkReadPart(
    const std::string& path,
    const Size2i& size, const Point2d& offset,
    double scale = 1, ResampleAlg alg = ResampleAlg::NearestNeighbour
)
{
    auto bc = std::make_shared<TLruBlockCache>();
    TDataset ds = OpenDataset(path);
    const Index bands = ds.Dim().Bands();
    Box2d box(offset, offset + size.cast<double>().matrix());
    const Size2i imgSize = (size.cast<double>() * scale).cast<int>();

    TestDataset<T> cDs(path, bc);
    Image<T> cImg(imgSize, bands);
    cDs.ds().Read(cImg, box, alg);

    Image<T> img(imgSize, bands);
    ds.Read(img, box, alg);

    EXPECT_EQ(cImg.maxAbsDifference(img), 0) << path << "; "
                    << size.transpose() << "; " << offset.transpose() << "; " << scale << "; " << alg;
}

} // namespace

Y_UNIT_TEST_SUITE(concurrent_dataset_should) {

Y_UNIT_TEST(read_all)
{
    checkReadAll<uint8_t>(IKONOS_PATH, 1);
    checkReadAll<uint8_t>(IKONOS_PATH, 1, ResampleAlg::Bilinear);
    checkReadAll<uint8_t>(MUL_PATH, 1);
    checkReadAll<uint8_t>(MUL_PATH, 1, ResampleAlg::Bilinear);
    checkReadAll<int32_t>(SRTM_PATH, 1);
    checkReadAll<int32_t>(SRTM_PATH, 1, ResampleAlg::Bilinear);
}

Y_UNIT_TEST(read_all_scaled_down)
{
    checkReadAll<uint8_t>(IKONOS_PATH, 0.75);
    checkReadAll<uint8_t>(IKONOS_PATH, 0.75, ResampleAlg::Bilinear);
    checkReadAll<uint8_t>(MUL_PATH, 0.75);
    checkReadAll<uint8_t>(MUL_PATH, 0.75, ResampleAlg::Bilinear);
    checkReadAll<int32_t>(SRTM_PATH, 0.75);
    checkReadAll<int32_t>(SRTM_PATH, 0.75, ResampleAlg::Bilinear);
}

Y_UNIT_TEST(read_all_scaled_up)
{
    checkReadAll<uint8_t>(IKONOS_PATH, 2);
    checkReadAll<uint8_t>(IKONOS_PATH, 2, ResampleAlg::Bilinear);
    checkReadAll<uint8_t>(MUL_PATH, 2);
    checkReadAll<uint8_t>(MUL_PATH, 2, ResampleAlg::Bilinear);
    checkReadAll<int32_t>(SRTM_PATH, 2);
    checkReadAll<int32_t>(SRTM_PATH, 2, ResampleAlg::Bilinear);
}

Y_UNIT_TEST(read_all_using_overview)
{
    checkReadAll<uint8_t>(IKONOS_PATH, 0.1);
    checkReadAll<uint8_t>(IKONOS_PATH, 0.1, ResampleAlg::Bilinear);
    checkReadAll<uint8_t>(IKONOS_BAND_PATH, 0.1);
    checkReadAll<uint8_t>(IKONOS_BAND_PATH, 0.1, ResampleAlg::Bilinear);
    checkReadAll<uint8_t>(MUL_PATH, 0.1);
    checkReadAll<uint8_t>(MUL_PATH, 0.1, ResampleAlg::Bilinear);
    checkReadAll<int32_t>(SRTM_PATH, 0.1);
    checkReadAll<int32_t>(SRTM_PATH, 0.1, ResampleAlg::Bilinear);
}

Y_UNIT_TEST(read_part_mul_using_overview)
{
    const Size2i size(392, 7423);
    const Size2i partSize(303, 301);
    const Size2d p = (size - partSize).cast<double>() - Size2d(0.5, 0.5);
    checkReadPart<uint8_t>(MUL_PATH, partSize, p, 0.2);
    checkReadPart<uint8_t>(MUL_PATH, partSize, p, 0.2, ResampleAlg::Bilinear);
    checkReadPart<uint8_t>(MUL_PATH, partSize, p, 0.2, ResampleAlg::Cubic);
}

Y_UNIT_TEST(read_part_mul)
{
    const Size2i size(392, 7423);
    for (Size2i partSize: {Size2i(31, 33), Size2i(301, 303)}) {
        const Point2d p = (size - partSize).cast<double>();
        const Point2d offsets[]{{0, 0}, {0.5, 0.5}, p, p - Point2d(0.5, 0.5), p - Point2d(11, 13)};
        for (double scale: {1.75, 1.0, 0.75, 0.25}) {
            for (Point2d offset: offsets) {
                checkReadPart<uint8_t>(MUL_PATH, partSize, offset, scale);
                checkReadPart<uint8_t>(MUL_PATH, partSize, offset, scale, ResampleAlg::Bilinear);
                checkReadPart<uint8_t>(MUL_PATH, partSize, offset, scale, ResampleAlg::Cubic);
            }
        }
    }
}

Y_UNIT_TEST(read_part_ikonos)
{
    const Size2i size(128, 120);
    const Size2i partSize(31, 33);
    const Point2d p = (size - partSize).cast<double>();
    const Point2d offsets[]{{0, 0}, {0.5, 0.5}, p, p - Point2d(0.5, 0.5), p - Point2d(11, 13)};
    for (double scale: {1.75, 1.0, 0.75, 0.25}) {
        for (Point2d offset: offsets) {
            checkReadPart<uint8_t>(IKONOS_PATH, partSize, offset, scale);
            checkReadPart<uint8_t>(IKONOS_PATH, partSize, offset, scale, ResampleAlg::Bilinear);
            checkReadPart<uint8_t>(IKONOS_PATH, partSize, offset, scale, ResampleAlg::Cubic);
            checkReadPart<uint8_t>(IKONOS_BAND_PATH, partSize, offset, scale);
            checkReadPart<uint8_t>(IKONOS_BAND_PATH, partSize, offset, scale, ResampleAlg::Bilinear);
            checkReadPart<uint8_t>(IKONOS_BAND_PATH, partSize, offset, scale, ResampleAlg::Cubic);
        }
    }
}

Y_UNIT_TEST(read_part_srtm)
{
    const Size2i size(1201, 1201);
    for (Size2i partSize: {Size2i(31, 33), Size2i(301, 303)}) {
        const Point2d p = (size - partSize).cast<double>();
        const Point2d offsets[]{{0, 0}, {0.5, 0.5}, p, p - Point2d(0.5, 0.5), p - Point2d(11, 13)};
        for (double scale: {1.75, 1.0, 0.75, 0.25}) {
            for (Point2d offset: offsets) {
                checkReadPart<uint8_t>(SRTM_PATH, partSize, offset, scale);
                checkReadPart<uint8_t>(SRTM_PATH, partSize, offset, scale, ResampleAlg::Bilinear);
                checkReadPart<uint8_t>(SRTM_PATH, partSize, offset, scale, ResampleAlg::Cubic);
            }
        }
    }
}

} // suite
} //namespace maps::factory::dataset::tests
