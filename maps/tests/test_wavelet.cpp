#include <maps/factory/libs/image/wavelet.h>

#include <maps/factory/libs/dataset/memory_file.h>
#include <maps/factory/libs/dataset/create_raster_dataset.h>
#include <maps/factory/libs/dataset/vrt_dataset.h>
#include <maps/factory/libs/image/statistics.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;
using namespace image;

using Eigen::MatrixXf;
using Eigen::VectorXf;
using Eigen::Vector3f;

Y_UNIT_TEST_SUITE(wavelet_fusion_should) {

Y_UNIT_TEST(scale_down_and_fuse_back)
{
    VsiPath pathPan = std::string("./tmp/") + this->Name_ + "_pan.tif";
    VsiPath pathMul = std::string("./tmp/") + this->Name_ + "_mul.tif";
    VsiPath pathFused = std::string("./tmp/") + this->Name_ + "_fused.tif";

    TDataset srcDs = OpenDataset(IKONOS_PATH);
    const Size2i panSz = srcDs.size();
    const FloatImage srcImg = srcDs.Read<float>();

    constexpr int scale = 4;
    const Size2i mulSz = panSz / scale;

    const Vector3f beta(0.25, 0.4, 0.35);
    FloatImage panImg(panSz, 1);
    panImg.array() = (srcImg.array().matrix() * beta).array();

    {
        TDataset pan = CreateTiff(pathPan).setSize(panSz).setBands(1).setType(TDataType::Float32);
        pan.copyInfoFrom(srcDs);
        TDataset mul = CreateTiff(pathMul).setSize(mulSz).setBands(3).setType(TDataType::Float32);
        mul.copyInfoFrom(srcDs);
        mul.setPixelSize(srcDs.Site().PixelSize() * scale);

        pan.Write(panImg);
        mul.Write(srcImg, Box2d(Point2d::Zero(), mulSz.cast<double>()), ResampleAlg::Average);
    }

    TDataset pan = OpenDataset(pathPan);
    TDataset mul = OpenDataset(pathMul);
    VrtDataset vrt(panSz);
    vrt.copyInfoFrom(srcDs);
    vrt.add(mul, ResampleAlg::CubicSpline);
    vrt.add(pan);

    TDataset resDs = CreateTiff(pathFused).like(pan).setBands(3);

    FloatImage fusedBlock(vrt.BlockSize(), 3);
    vrt.ForEachBlock<float>({}, [&](FloatImageBase& img, const auto& origin) {
        UNIT_ASSERT_NO_EXCEPTION(wavelet(img, fusedBlock, 3));
        resDs.Write(fusedBlock, origin);
    });

    FloatImage resImg = resDs.Read<float>();
    EXPECT_LE(resImg.normDifference(srcImg), 2500);
    EXPECT_LE(resImg.meanAbsDifference(srcImg), 10);
    EXPECT_LE(resImg.maxAbsDifference(srcImg), 100);
}

} // suite

} //namespace maps::factory::dataset::tests

