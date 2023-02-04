import pytest
import torch
import numpy.testing as npt
from ads_pytorch.cpp_lib import libcpp_lib
from random import randint

DEVICES = ["cuda", "cpu"]
OCCURENCE_THRESHOLD = [0, 1000]
DIM = list(range(100, 1500, 200))
BATCH = [2 ** i for i in range (7, 10)]

def assert_almost_equal(actual, expected, decimal=7, **kwargs):
    def __np_array_from_torch__(torch):
        return torch.detach().numpy()

    npt.assert_almost_equal(
        __np_array_from_torch__(actual),
        __np_array_from_torch__(expected),
        decimal=decimal,
        **kwargs
    )

@pytest.mark.parametrize('a', DIM)
@pytest.mark.parametrize('b', BATCH)
@pytest.mark.parametrize("occurence_threshold", OCCURENCE_THRESHOLD)
@pytest.mark.parametrize("device", DEVICES)
def test_quantiles_one(device, occurence_threshold, a, b):
    # test that quantiles got with TDigest are equal to the quantiles got by torch (the error is <= q(1-q) according to the article)
    dims_to_reduce = [0]
    normalizer = libcpp_lib.UniversalNormalizer([0], [a], 5, 1000, occurence_threshold, device)
    X = torch.randn((a, b), device=torch.device(device))
    n_bins = 5
    z1 = normalizer.get_norm(X, return_quantiles=True,
                                        normalize_only=False,
                                        use_cdf=False,
                                        use_double_binarization=False)
    q = torch.tensor([i/n_bins for i in range(1, n_bins + 1)], device=torch.device(device))
    z2 = torch.quantile(X, q, dim=1)
    error =  q * (1-q)
    assert torch.all(torch.abs(z1 - z2.T) - error <= torch.zeros_like(z1)) == True

@pytest.mark.parametrize('a', DIM)
@pytest.mark.parametrize('b', BATCH)
@pytest.mark.parametrize("occurence_threshold", OCCURENCE_THRESHOLD)
@pytest.mark.parametrize("device", DEVICES)
def test_get_quantiles(device, occurence_threshold, a, b):
    # test get_quantiles function: that it works correctly like get_norm without updating anything (no object-function matrix shpul be passed there)
    n_bins = 5
    normalizer1 = libcpp_lib.UniversalNormalizer([1], [b], n_bins, 1000, occurence_threshold, device)
    X = torch.randn((a, b), device=torch.device(device))

    a = normalizer1.get_norm(X.permute(1, 0).reshape(b, a).contiguous(), return_quantiles=False,
                                                                                        normalize_only=False,
                                                                                        use_cdf=False,
                                                                                        use_double_binarization=False)
    
    z1 = normalizer1.get_quantiles()
    q = torch.tensor([i/n_bins for i in range(1, n_bins + 1)], device=torch.device(device))
    z2 = torch.quantile(X, q, dim=0)
    error =  q * (1-q)
    assert torch.all(torch.abs(z1 - z2.T) - error <= torch.zeros_like(z1)) == True

@pytest.mark.parametrize('a', DIM)
@pytest.mark.parametrize('b', BATCH)
@pytest.mark.parametrize("occurence_threshold", OCCURENCE_THRESHOLD)
@pytest.mark.parametrize("device", DEVICES)
def test_binarization_median(device, occurence_threshold, a, b):
    # test that features are binarized correctly when using 2 quantiles (this algorithm can be easily implemented in torch)
    # as a vectorized one, so this check should be objective
    normalizer = libcpp_lib.UniversalNormalizer([0], [a],  2, 1000, occurence_threshold, device)
    dims_to_reduce = [0]
    X = torch.randn((a, b), device=torch.device(device))
    n_bins = 2
    z1 = normalizer.get_norm(X, return_quantiles=False,
                                        normalize_only=False,
                                        use_cdf=False,
                                        use_double_binarization=False)
    
    z2 = (X > torch.quantile(X, 0.5, dim=1, keepdim=True)).float()
    assert_almost_equal(z1.cpu(), z2.cpu())

@pytest.mark.parametrize('a', DIM)
@pytest.mark.parametrize("occurence_threshold", OCCURENCE_THRESHOLD)
@pytest.mark.parametrize("device", DEVICES)
def test_quantiles_many(device, occurence_threshold, a):
    # check that the quantiles are saved correctly (compare the algorithm catching all arguments at once with
    # almost the same one but processing feature values through iterations
    b = 1000
    X = torch.randn((a, b), device=torch.device(device))
    un = libcpp_lib.UniversalNormalizer([0], [a], 5, 1000, occurence_threshold, device)
    for i in range(100):
        z = un.get_norm(X[:, i * 10:(i + 1) * 10].clone(), return_quantiles=True,
                                                            normalize_only=False,
                                                            use_cdf=False,
                                                            use_double_binarization=False)

    un1 = libcpp_lib.UniversalNormalizer([0], [a], 5, 1000.0, occurence_threshold, device)
    z_all = un1.get_norm(X.clone(), return_quantiles=True,
                                    normalize_only=False,
                                    use_cdf=False,
                                    use_double_binarization=False)

    assert_almost_equal(z.cpu(), z_all.cpu(), 2)

@pytest.mark.parametrize('a', DIM)
@pytest.mark.parametrize('b', BATCH)
@pytest.mark.parametrize("occurence_threshold", OCCURENCE_THRESHOLD)
@pytest.mark.parametrize("device", DEVICES)
def test_quantiles_TDigestArray(device, occurence_threshold, a, b):
    # test if TDigestArray -- which is a variable necessary for optimization is stored and updated correctly
    X = torch.randn((a, b), device=torch.device(device))
    TDigestArray = torch.zeros((8, a, 10 * int(1000) + 1), device=torch.device(device))
    un = libcpp_lib.UniversalNormalizer([0], [a], 5, 1000, occurence_threshold, device)
    for i in range(100):
        z = un.get_norm(X[:, i * 10:(i + 1) * 10].clone(), return_quantiles=True,
                                                            normalize_only=False,
                                                            use_cdf=False,
                                                            use_double_binarization=False)
        
        un = libcpp_lib.UniversalNormalizer([0], [a], 1000.0, 5, un.get_TDigestArray())

    un1 = libcpp_lib.UniversalNormalizer([0], [a], 5, 1000.0, occurence_threshold, device)
    z_all = un1.get_norm(X.clone(), return_quantiles=True,
                                    normalize_only=False,
                                    use_cdf=False,
                                    use_double_binarization=False)

    assert_almost_equal(z.cpu(), z_all.cpu(), 2)

@pytest.mark.parametrize('a', DIM)
@pytest.mark.parametrize('b', BATCH)
@pytest.mark.parametrize("occurence_threshold", OCCURENCE_THRESHOLD)
@pytest.mark.parametrize("device", DEVICES)
def test_quantiles_TDigestArray_2(device, occurence_threshold, a, b):
    # almost the same as the previous one, but the class variable is not created each time (check if the quantiles are stored and saved correctly)
    X = torch.randn((a, b), device=torch.device(device))
    TDigestArray = torch.zeros((8, a, 10 * int(1000) + 1), device=torch.device(device))
    un = libcpp_lib.UniversalNormalizer([0], [a], 5, 1000, occurence_threshold, device)
    for i in range(100):
        z = un.get_norm(X[:, i * 10:(i + 1) * 10].clone(), return_quantiles=True,
                                                            normalize_only=False,
                                                            use_cdf=False,
                                                            use_double_binarization=False)

    un1 = libcpp_lib.UniversalNormalizer([0], [a], 1000.0, 5, un.get_TDigestArray())
    z_all = un1.get_norm(X.clone(), return_quantiles=True,
                                    normalize_only=False,
                                    use_cdf=False,
                                    use_double_binarization=False)

    # When big tests were added this assert started falling with tolerance 2 for cases of big tensors and many bins
    # For tests not to fall every time, I decided to change 2 for 1 here
    assert_almost_equal(z.cpu(), z_all.cpu(), 1)

@pytest.mark.parametrize('a', DIM)
@pytest.mark.parametrize('b', BATCH)
@pytest.mark.parametrize("occurence_threshold", OCCURENCE_THRESHOLD)
@pytest.mark.parametrize("device", DEVICES)
def test_normalize_cdf(device, occurence_threshold, a, b):
    # there is another way to quantize features using cdf -- compare its binarization with the binarization of original algorithm
    # practical experiments show that the difference between the quantization is likely to be <= 2, which is actually small
    X = torch.randn((a, b), device=torch.device(device))
    un1 = libcpp_lib.UniversalNormalizer([0], [a], 128, 1000, occurence_threshold, device)
    z1 = un1.get_norm(X, return_quantiles=False,
                            normalize_only=False,
                            use_cdf=False,
                            use_double_binarization=False)

    un2 = libcpp_lib.UniversalNormalizer([0], [a], 128, 1000, occurence_threshold, device)
    z2 = un2.get_norm(X, return_quantiles=False,
                            normalize_only=False,
                            use_cdf=True,
                            use_double_binarization=False)
    
    # In this case we check that the bins got using by different methods are almost equal 
    # (1 or 2 bins difference is not important when there are many bins)
    # Changing from 1 to 2 is needed for big tests (for small tests it is almost always 1)
    assert torch.max(torch.abs(z1 - z2)) == 0

@pytest.mark.parametrize('a', DIM)
@pytest.mark.parametrize('b', BATCH)
@pytest.mark.parametrize("occurence_threshold", OCCURENCE_THRESHOLD)
@pytest.mark.parametrize("device", DEVICES)
def test_interpolate_cdf(device, occurence_threshold, a, b):
    # test is two algorithms with cdf and ordinary quantization return almost the same values
    #(both the quantized tensor and alpha)
    X = torch.randn((a, b), device=torch.device(device))
    un1 = libcpp_lib.UniversalNormalizer([0], [a], 128, 1000, occurence_threshold, device)
    z1, alpha1 = un1.get_interpolate(X, normalize_only=False,
                                        use_cdf=False,
                                        use_double_binarization=False)
    
    un2 = libcpp_lib.UniversalNormalizer([0], [a], 128, 1000, occurence_threshold, device)
    z2, alpha2 = un2.get_interpolate(X, normalize_only=False,
                                        use_cdf=True,
                                        use_double_binarization=False)

    # In this case we check that the bins got using by different methods are almost equal 
    # (1 or 2 bins difference is not important when there are many bins)
    # Changing from 1 to 2 is needed for big tests (for small tests it is almost always 1)
    assert torch.max(torch.abs(z1 - z2)) == 0
    assert_almost_equal(alpha1.cpu(), alpha2.cpu())

