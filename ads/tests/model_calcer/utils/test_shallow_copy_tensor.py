import torch
from ads_pytorch.model_calcer.utils.shallow_copy_tensor import shallow_copy_tensor_structure


def _check_shallow_copy(tensor: torch.Tensor, shallow_copy: torch.Tensor):
    assert shallow_copy.requires_grad == tensor.requires_grad
    assert shallow_copy.dtype == tensor.dtype
    assert shallow_copy.device == tensor.device
    assert shallow_copy.storage().data_ptr() == tensor.storage().data_ptr()
    assert shallow_copy.size() == tensor.size()
    assert shallow_copy.stride() == tensor.stride()
    assert shallow_copy.numel() == tensor.numel()

    assert id(shallow_copy) != id(tensor)


def test_shallow_copy_tensor():
    tensor = torch.zeros(3, 3)
    shallow_copy = shallow_copy_tensor_structure(tensor)
    _check_shallow_copy(tensor=tensor, shallow_copy=shallow_copy)

    shallow_copy2 = shallow_copy_tensor_structure(tensor)
    _check_shallow_copy(tensor=tensor, shallow_copy=shallow_copy2)

    shallow_copy[0][0] = 1
    assert tensor[0][0] == 1

    _check_shallow_copy(tensor=tensor, shallow_copy=shallow_copy2)
    _check_shallow_copy(tensor=shallow_copy, shallow_copy=shallow_copy2)
    assert torch.allclose(shallow_copy2, tensor)
