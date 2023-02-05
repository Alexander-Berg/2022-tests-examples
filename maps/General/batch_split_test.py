import random

from maps.poi.notification.lib.ugc_account import get_batch_split


def check_batch_split(size, n_batches):
    # example: get_batch_split(22, 5) == [(0, 5), (5, 10), (10, 15), (15, 20), (20, 22)]
    batches = list(get_batch_split(size, n_batches))

    assert len(batches) == n_batches

    for batch in batches:
        assert batch[0] <= batch[1]

    # check every index belongs to exactly one batch
    for i in range(size):
        assert sum(map(lambda batch: batch[0] <= i < batch[1], batches)) == 1

    def length(batch):
        return batch[1] - batch[0]

    # check batch sizes are approximetely equal
    for i in range(n_batches - 1):
        assert abs(length(batches[i]) - length(batches[i + 1])) < n_batches
    assert abs(length(batches[0]) - length(batches[-1])) < n_batches


def test_batch_split():
    check_batch_split(1234, 5)
    check_batch_split(1234, 2)
    check_batch_split(1234, 1)

    check_batch_split(1, 100)
    check_batch_split(1, 1)

    random.seed(42)
    for i in range(100):
        size = random.randint(1, 100)
        n_batches = random.randint(1, 16)
        check_batch_split(size, n_batches)
