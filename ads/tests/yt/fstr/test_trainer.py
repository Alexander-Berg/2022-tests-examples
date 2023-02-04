import torch

from ads_pytorch.yt.fstr.trainer import shuffle_feature, infer_batch_size


def test_shuffle_cat_feature():
    keys = torch.IntTensor([100, 101, 102, 103, 104, 105, 106])
    lens = torch.IntTensor([3, 0, 4, 0])
    order = torch.LongTensor([1, 2, 3, 0])
    feature = (keys, lens)

    shuffled_keys, shuffled_lens = shuffle_feature(feature, order)

    assert torch.equal(shuffled_keys, torch.IntTensor([103, 104, 105, 106, 100, 101, 102]))
    assert torch.equal(shuffled_lens, torch.IntTensor([0, 4, 0, 3]))


def test_shuffle_cat_seq_feature():
    keys = torch.IntTensor([100, 101, 102, 103, 104, 105, 106])
    lens = torch.IntTensor([[3, 0], [4, 0]])
    order = torch.LongTensor([1, 0])
    feature = (keys, lens)

    shuffled_keys, shuffled_lens = shuffle_feature(feature, order)

    assert torch.equal(shuffled_keys, torch.IntTensor([103, 104, 105, 106, 100, 101, 102]))
    assert torch.equal(shuffled_lens, torch.IntTensor([[4, 0], [3, 0]]))


def test_infer_batch_size():
    rv_batch = torch.Tensor([1.0, 2.0, 3.0])
    cat_batch = (torch.LongTensor([103, 104, 105, 106, 100, 101, 102]), torch.LongTensor([4, 3]))
    dict_batch = {
        "feature1": cat_batch
    }

    assert infer_batch_size(rv_batch) == 3
    assert infer_batch_size(cat_batch) == 2
    assert infer_batch_size(dict_batch) == 2
