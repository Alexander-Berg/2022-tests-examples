import os
import mock
import json
import uuid
import pytest

from infra.qyp.vmagent.src import volume_manager as volume_manager_module
from infra.qyp.proto_lib import vmagent_pb2


def test_qemu_img_cmd(tmpdir):
    check_output_mock = mock.Mock()

    qemu_img_cmd = volume_manager_module.QEMUImgCmd(sudo=True)
    assert qemu_img_cmd._bin_path == ['sudo', volume_manager_module.QEMUImgCmd.DEFAULT_BIN_PATH]

    qemu_img_cmd = volume_manager_module.QEMUImgCmd()
    assert qemu_img_cmd._bin_path == [volume_manager_module.QEMUImgCmd.DEFAULT_BIN_PATH]

    qemu_img_cmd._check_output = check_output_mock
    fake_img_path = str(tmpdir.join('fake_image'))
    fake_delta_path = str(tmpdir.join('fake_delta'))

    # case: info
    check_output_mock.return_value = json.dumps({'test': 1})

    qemu_img_cmd.info(fake_img_path)
    check_output_mock.assert_called_with([volume_manager_module.QEMUImgCmd.DEFAULT_BIN_PATH,
                                          'info', fake_img_path, '--output', 'json'], stderr=mock.ANY)

    # case: get_virtual_size
    check_output_mock.return_value = json.dumps({'virtual-size': 100})
    success, res = qemu_img_cmd.get_virtual_disk_size(fake_img_path)
    assert success and res == 100
    check_output_mock.assert_called_with([volume_manager_module.QEMUImgCmd.DEFAULT_BIN_PATH,
                                          'info', fake_img_path, '--output', 'json'], stderr=mock.ANY)

    # case: resize
    check_output_mock.return_value = ''
    qemu_img_cmd.resize(fake_img_path, '10G')
    check_output_mock.assert_called_with([volume_manager_module.QEMUImgCmd.DEFAULT_BIN_PATH,
                                          'resize', fake_img_path, '10G'], stderr=mock.ANY)

    # case: create empty
    qemu_img_cmd.create(fake_img_path, '10G')
    check_output_mock.assert_called_with([volume_manager_module.QEMUImgCmd.DEFAULT_BIN_PATH,
                                          'create', '-f', 'qcow2', fake_img_path, '10G'], stderr=mock.ANY)

    # case: create delta
    qemu_img_cmd.create(fake_delta_path, '10G', backing_file=fake_img_path)
    check_output_mock.assert_called_with([volume_manager_module.QEMUImgCmd.DEFAULT_BIN_PATH,
                                          'create', '-f', 'qcow2', fake_delta_path, '10G', '-b', fake_img_path],
                                         stderr=mock.ANY)

    # case: rebase delta
    qemu_img_cmd.rebase(fake_delta_path, fake_img_path)
    check_output_mock.assert_called_with([volume_manager_module.QEMUImgCmd.DEFAULT_BIN_PATH,
                                          'rebase', '-u', '-b', fake_img_path, fake_delta_path], stderr=mock.ANY)


def test_volume_wrapper(tmpdir):
    vm_mount_dir = tmpdir
    vm_volume = vmagent_pb2.VMVolume(mount_path=str(vm_mount_dir))
    v = volume_manager_module.VolumeWrapper(vm_volume)

    assert v.image_folder_path == vm_mount_dir.join(volume_manager_module.VolumeWrapper.IMAGE_FOLDER_NAME)

    assert not v.is_main
    vm_volume.is_main = True
    assert v.is_main

    assert v.is_empty
    vm_volume.resource_url = 'rbtorrent:anyurl'
    assert not v.is_empty

    vm_volume.image_type = vmagent_pb2.VMVolume.RAW
    assert v.image_type_is_raw and not v.image_type_is_delta
    assert v.image_file_path == vm_mount_dir.join(volume_manager_module.VolumeWrapper.IMAGE_FILE_NAME)
    assert v.drive_path == v.image_file_path

    assert not v.image_file_exist
    vm_mount_dir.join(volume_manager_module.VolumeWrapper.IMAGE_FILE_NAME).write('test')
    assert v.image_file_exist

    vm_volume.image_type = vmagent_pb2.VMVolume.DELTA
    assert not v.image_type_is_raw and v.image_type_is_delta
    assert v.image_file_path == vm_mount_dir.join(volume_manager_module.VolumeWrapper.IMAGE_FILE_NAME)
    assert v.delta_file_path == vm_mount_dir.join(volume_manager_module.VolumeWrapper.DELTA_FILE_NAME)
    assert v.drive_path == v.delta_file_path

    assert not v.delta_file_exist
    vm_mount_dir.join(volume_manager_module.VolumeWrapper.DELTA_FILE_NAME).write('test')
    assert v.delta_file_exist

    vm_volume.is_main = True
    assert v.vm_device_name == '/dev/vda'

    vm_volume.order = 1
    vm_volume.is_main = False
    assert v.vm_device_name == '/dev/vdc'

    vm_volume.name = 'test'
    assert v.vm_mount_path == '/extra_test'
    vm_volume.vm_mount_path = '/any_path'
    assert v.vm_mount_path == '/any_path'

    vm_volume.available_size = 100
    assert v.available_size == 100


def test_volume_wrapper_equal():
    vm_volume_1 = vmagent_pb2.VMVolume(name='/qemu-persistent',
                                       resource_url='qdm:any',
                                       image_type=vmagent_pb2.VMVolume.RAW)
    vm_volume_2 = vmagent_pb2.VMVolume()
    vm_volume_2.CopyFrom(vm_volume_1)

    v1 = volume_manager_module.VolumeWrapper(vm_volume_1)
    v2 = volume_manager_module.VolumeWrapper(vm_volume_2)
    assert v1 == v2

    vm_volume_2.req_id = 'any'
    vm_volume_2.mount_path = 'any'
    vm_volume_2.is_main = True
    vm_volume_2.order = 100
    assert v1 == v2
    assert {v1.name: v1} == {v2.name: v2}

    vm_volume_2.resource_url = 'qdm:any2'
    assert not v1 == v2
    assert not {v1.name: v1} == {v2.name: v2}


@pytest.fixture
def volume_mount_dir(tmpdir):
    return tmpdir


@pytest.fixture
def volume_pb():
    """
    Used for set volume pb values

    """
    return vmagent_pb2.VMVolume()


@pytest.fixture
def volume(volume_mount_dir, volume_pb):
    volume_pb.mount_path = str(volume_mount_dir)
    volume_pb.name = 'test'
    return volume_manager_module.VolumeWrapper(volume_pb)


@pytest.fixture
def resource_manager_mock():
    return mock.Mock()


@pytest.fixture
def qemu_img_mock():
    return mock.Mock()


@pytest.fixture
def volume_manager(volume, resource_manager_mock, qemu_img_mock):
    return volume_manager_module.VolumeManager(volume, resource_manager_mock, qemu_img_mock)


def test_remove_all_files(volume_manager, volume, volume_mount_dir):
    image_file = volume_mount_dir.join(volume.IMAGE_FILE_NAME)
    image_file.write('test')
    assert os.path.exists(str(image_file))
    delta_file = volume_mount_dir.join(volume.DELTA_FILE_NAME)
    delta_file.write('test')
    assert os.path.exists(str(delta_file))

    any_file_in_image_folder = volume_mount_dir.join(volume.IMAGE_FOLDER_NAME).join('test')
    any_file_in_image_folder.write('test', ensure=True)
    assert os.path.exists(str(any_file_in_image_folder))

    volume_manager.remove_all_files()

    assert not os.path.exists(str(image_file))
    assert not os.path.exists(str(delta_file))
    assert not os.path.exists(str(volume_mount_dir.join(volume.IMAGE_FOLDER_NAME)))
    assert not os.path.exists(str(any_file_in_image_folder))


def test_fix_image_location(volume_manager, volume, volume_mount_dir):
    # case: image files store in volume mount_path
    volume_manager.remove_all_files()
    image_file = volume_mount_dir.join(volume.IMAGE_FILE_NAME)
    image_file.write('test')
    assert os.path.exists(str(image_file))

    delta_file = volume_mount_dir.join(volume.DELTA_FILE_NAME)
    delta_file.write('test')
    assert os.path.exists(str(delta_file))

    image_folder = volume_mount_dir.join(volume.IMAGE_FOLDER_NAME)
    image_folder_path = str(image_folder)

    assert not os.path.exists(image_folder_path)

    volume_manager.fix_image_location()

    assert os.path.exists(image_folder_path)
    assert os.path.islink(str(image_file))
    assert os.path.islink(str(delta_file))

    assert os.path.realpath(str(image_file)).startswith(image_folder_path)
    assert os.path.realpath(str(delta_file)).startswith(image_folder_path)

    # case: image file store in volume mount_path and image_folder
    volume_manager.remove_all_files()
    image_file = volume_mount_dir.join(volume.IMAGE_FILE_NAME)
    image_file.write('test')
    assert os.path.exists(str(image_file))

    delta_file = volume_mount_dir.join(volume.DELTA_FILE_NAME)
    delta_file.write('test')
    assert os.path.exists(str(delta_file))

    another_image_file = image_folder.join(volume.IMAGE_FILE_NAME)
    another_image_file.write('test', ensure=True)
    assert os.path.exists(str(another_image_file))

    volume_manager.fix_image_location()

    assert os.path.exists(str(another_image_file))

    assert os.path.exists(image_folder_path)
    assert os.path.islink(str(image_file))
    assert os.path.islink(str(delta_file))

    assert os.path.realpath(str(image_file)).startswith(image_folder_path)
    assert os.path.realpath(str(delta_file)).startswith(image_folder_path)

    # case: delta file store in volume mount_path and image_folder
    volume_manager.remove_all_files()
    image_file = volume_mount_dir.join(volume.IMAGE_FILE_NAME)
    image_file.write('test')
    assert os.path.exists(str(image_file))

    delta_file = volume_mount_dir.join(volume.DELTA_FILE_NAME)
    delta_file.write('test')
    assert os.path.exists(str(delta_file))

    another_delta_file = image_folder.join(volume.DELTA_FILE_NAME)
    another_delta_file.write('test', ensure=True)
    assert os.path.exists(str(another_delta_file))

    with pytest.raises(volume_manager.BadAction) as e:
        volume_manager.fix_image_location()

    assert e.value.message == 'We have another delta file in image_folder/, unable to move current'


def test_find_images_with_rundom_resource_dir(volume_pb, volume_mount_dir, volume, volume_manager):
    random_dir = str(uuid.uuid4())
    test_folder = volume_mount_dir.join(volume.IMAGE_FOLDER_NAME).join(random_dir)
    layer_file = test_folder.join('layer')
    layer_file.write('1', ensure=True)
    current_qcow_file = test_folder.join('current.qcow2')
    current_qcow_file.write('1', ensure=True)

    image_file, delta_file, unused_files = volume_manager.find_images()

    assert image_file == str(layer_file)
    assert delta_file == str(current_qcow_file)


def test_validate_image_size(volume_pb, volume_mount_dir, volume, volume_manager, qemu_img_mock):
    image_path = str(volume_mount_dir.join(volume.IMAGE_FILE_NAME))
    delta_path = str(volume_mount_dir.join(volume.DELTA_FILE_NAME))
    # case: get_virtual_disk_size return error
    qemu_img_mock.get_virtual_disk_size.return_value = False, Exception('any execption')
    with pytest.raises(volume_manager.ImageError) as error:
        volume_manager.validate_image_size(image_path)
    assert 'any execption' in error.value.message
    # case: pass only image_file, with valid virtual size
    qemu_img_mock.get_virtual_disk_size.return_value = True, volume.available_size
    volume_manager.validate_image_size(image_path)

    # case: pass only image_file, with not valid virtual size
    volume_pb.available_size = 100
    qemu_img_mock.get_virtual_disk_size.return_value = True, volume.available_size + 1
    with pytest.raises(volume_manager.VirtualSizeNotValidError) as e:
        volume_manager.validate_image_size(image_path)

    assert e.value.message == "Available size for disk '{}' does not enough for this image:" \
                              " should be at least (101), got(100)".format(volume_pb.name)

    # case: pass image_file and delta_file, with valid virtual size
    volume_pb.available_size = 100
    with mock.patch('infra.qyp.vmagent.src.helpers.get_image_size') as get_image_size_mock:
        get_image_size_mock.return_value = 3
        qemu_img_mock.get_virtual_disk_size.return_value = (
            True, volume.available_size - get_image_size_mock.return_value)

        volume_manager.validate_image_size(image_path, delta_path)

        get_image_size_mock.assert_called_with(image_path)
        qemu_img_mock.get_virtual_disk_size.assert_called_with(delta_path)

    # case: pass image_file and delta_file, with not valid virtual size
    volume_pb.available_size = 100
    with mock.patch('infra.qyp.vmagent.src.helpers.get_image_size') as get_image_size_mock:
        get_image_size_mock.return_value = 3
        qemu_img_mock.get_virtual_disk_size.return_value = True, volume.available_size

        with pytest.raises(volume_manager.VirtualSizeNotValidError) as e:
            volume_manager.validate_image_size(image_path, delta_path)

        assert e.value.message == "Available size for disk 'test' does not enough for this image:" \
                                  " should be at least (103), got(100)"


def test_download_image(volume_mount_dir, volume, volume_manager, qemu_img_mock, resource_manager_mock):
    fake_resource_url = 'rbtorrent:any_url'

    # case: no files downloaded
    with pytest.raises(volume_manager.DownloadingImageError) as e:
        volume_manager.download_image(fake_resource_url)

    assert e.value.message == 'Unable to find image file (no files?)'

    # case: download only image file (random name)
    volume_manager.clear_image_folder()
    image_file_name = "{}.img".format(uuid.uuid4())

    def get_resource(_, target_folder):
        with open(os.path.join(target_folder, image_file_name), 'w') as fp:
            fp.write('1')

    resource_manager_mock.get_resource.side_effect = get_resource
    image_file, delta_file = volume_manager.download_image(fake_resource_url)
    assert image_file and not delta_file
    resource_manager_mock.get_resource.assert_called_with(fake_resource_url, volume.image_folder_path)

    # case: download only delta file
    volume_manager.clear_image_folder()
    delta_file_name = volume.DELTA_FILE_NAME

    def get_resource(_, target_folder):
        with open(os.path.join(target_folder, delta_file_name), 'w') as fp:
            fp.write('1')

    resource_manager_mock.get_resource.side_effect = get_resource
    image_file, delta_file = volume_manager.download_image(fake_resource_url)
    assert image_file and not delta_file

    # case: download image and delta file
    volume_manager.clear_image_folder()
    image_file_name = "{}.img".format(uuid.uuid4())
    delta_file_name = volume.DELTA_FILE_NAME

    def get_resource(_, target_folder):
        with open(os.path.join(target_folder, image_file_name), 'w') as fp:
            fp.write('1')
        with open(os.path.join(target_folder, delta_file_name), 'w') as fp:
            fp.write('1')

    resource_manager_mock.get_resource.side_effect = get_resource
    image_file, delta_file = volume_manager.download_image(fake_resource_url)
    assert image_file and delta_file

    # case: download many image files and delta file
    volume_manager.clear_image_folder()
    image_file_1_name = "a.img"
    image_file_2_name = "b.img"
    delta_file_name = volume.DELTA_FILE_NAME

    def get_resource(_, target_folder):
        with open(os.path.join(target_folder, image_file_1_name), 'w') as fp:
            fp.write('1')
        with open(os.path.join(target_folder, image_file_2_name), 'w') as fp:
            fp.write('1')
        with open(os.path.join(target_folder, delta_file_name), 'w') as fp:
            fp.write('1')

    resource_manager_mock.get_resource.side_effect = get_resource
    with pytest.raises(volume_manager.DownloadingImageError) as error:
        volume_manager.download_image(fake_resource_url)
    assert 'To many files has been downloaded' in error.value.message


def test_create_image(volume_pb, volume_mount_dir, volume, volume_manager, qemu_img_mock):
    # case: create RAW image
    volume_pb.available_size = 100
    volume_pb.image_type = vmagent_pb2.VMVolume.RAW

    def create_image(layer_file_path, disk_size, backing_file=None):
        with open(layer_file_path, 'w') as fp:
            fp.write('1')
        return True, None

    qemu_img_mock.create.side_effect = create_image
    result_file = volume_manager.create_image()
    assert result_file == os.path.join(volume.image_folder_path, volume.IMAGE_FILE_NAME)
    qemu_img_mock.create.assert_called_with(result_file, volume.available_size, backing_file=None)

    with pytest.raises(volume_manager.ImageError) as e:
        volume_manager.create_image()

    assert e.value.message == "Can't create image , file '{}' already exists".format(result_file)

    # case: create DELTA image not in main volume
    volume_manager.remove_all_files()
    volume_pb.image_type = vmagent_pb2.VMVolume.DELTA
    volume_pb.is_main = False

    with pytest.raises(volume_manager.CreateImageError) as e:
        volume_manager.create_image(str(volume_mount_dir.join('test')))

    assert e.value.message == 'Extra volume does not support create delta image'

    # case: create DELTA image
    volume_pb.available_size = 100
    volume_pb.is_main = True
    volume_pb.image_type = vmagent_pb2.VMVolume.DELTA
    image_file = volume_mount_dir.join(volume.IMAGE_FOLDER_NAME).join(volume.IMAGE_FILE_NAME)

    def create_image(layer_file_path, disk_size, backing_file=None):
        with open(layer_file_path, 'w') as fp:
            fp.write('1')

        return True, None

    qemu_img_mock.create.side_effect = create_image
    with pytest.raises(volume_manager.CreateImageError) as e:
        volume_manager.create_image()
    assert e.value.message == 'backing_file required for image_type == DELTA'

    with pytest.raises(volume_manager.CreateImageError) as e:
        volume_manager.create_image(str(image_file))
    assert e.value.message == "backing_file: '{}' does not exists".format(image_file)

    image_file.write('test', ensure=True)
    result_file = volume_manager.create_image(str(image_file))

    assert result_file == os.path.join(volume.image_folder_path, volume.DELTA_FILE_NAME)
    qemu_img_mock.create.assert_called_with(result_file, volume.available_size - image_file.size(),
                                            backing_file=str(image_file))


def test_create_image_symlinks(volume_mount_dir, volume, volume_manager):
    assert not volume.image_file_exist
    assert not volume.delta_file_exist

    # case: default
    image_file = volume_mount_dir.join(volume.IMAGE_FOLDER_NAME).join(volume.IMAGE_FILE_NAME)
    image_file.write('1', ensure=True)
    os.symlink(str(image_file), volume.image_file_path)

    delta_file = volume_mount_dir.join(volume.IMAGE_FOLDER_NAME).join(volume.DELTA_FILE_NAME)
    delta_file.write('1', ensure=True)
    os.symlink(str(delta_file), volume.delta_file_path)

    volume_manager.create_image_symlinks(str(image_file), str(delta_file))
    assert volume.image_file_exist and os.path.islink(volume.image_file_path)
    assert volume.delta_file_exist and os.path.islink(volume.delta_file_path)

    # case: image_file exists and is not symlink
    volume_manager.remove_all_files()
    image_file = volume_mount_dir.join(volume.IMAGE_FILE_NAME)
    image_file.write('1', ensure=True)
    with pytest.raises(volume_manager.ImageError) as e:
        volume_manager.create_image_symlinks(str(image_file))
    assert e.value.message == "Can't create image symlink, image exists and is not symlink: {}".format(image_file)

    # case: delta_file exists and is not symlink
    volume_manager.remove_all_files()
    image_file = volume_mount_dir.join(volume.IMAGE_FOLDER_NAME).join(volume.IMAGE_FILE_NAME)
    image_file.write('1', ensure=True)

    delta_file = volume_mount_dir.join(volume.DELTA_FILE_NAME)
    delta_file.write('1', ensure=True)
    with pytest.raises(volume_manager.ImageError) as e:
        volume_manager.create_image_symlinks(str(image_file), str(delta_file))
    assert e.value.message == "Can't create delta symlink, delta exists and is not symlink: {}".format(delta_file)


def test_find_images(volume_mount_dir, volume, volume_manager):
    image_file, delta_file, unused_files = volume_manager.find_images()
    assert not image_file and not delta_file

    image_file = volume_mount_dir.join(volume.IMAGE_FOLDER_NAME).join(volume.IMAGE_FILE_NAME)
    image_file.write('1', ensure=True)

    image_file, delta_file, unused_files = volume_manager.find_images()
    assert image_file and not delta_file

    delta_file = volume_mount_dir.join(volume.IMAGE_FOLDER_NAME).join(volume.DELTA_FILE_NAME)
    delta_file.write('1', ensure=True)

    image_file, delta_file, unused_files = volume_manager.find_images()
    assert image_file and delta_file


def test_init_image_main_raw(volume_pb, volume, volume_manager, qemu_img_mock, volume_mount_dir):
    qemu_img_mock.resize.return_value = (True, '')

    volume_manager.create_image = mock.Mock()
    volume_manager.download_image = mock.Mock()
    volume_manager.validate_image_size = mock.Mock()
    volume_manager.create_image_symlinks = mock.Mock()

    fake_resource_url = 'any_url'
    volume_pb.available_size = 100

    volume_pb.is_main = True
    volume_pb.image_type = vmagent_pb2.VMVolume.RAW
    volume_pb.resource_url = ''

    # case: init empty
    with pytest.raises(volume_manager.ImageError) as e:
        volume_manager.init_image()
    assert e.value.message == "Main volume can't be empty (resource_url required)"

    volume_pb.resource_url = fake_resource_url
    # case: try init with exists image file
    image_file = volume_mount_dir.join(volume.IMAGE_FOLDER_NAME).join(volume.IMAGE_FILE_NAME)
    image_file.write('1', ensure=True)
    with pytest.raises(volume_manager.ImageError) as e:
        volume_manager.init_image()

    assert e.value.message == "Can't init RAW image, some file exists: {}".format(image_file)

    # case: init with resource url (image file has been downloaded)
    volume_manager.remove_all_files()
    volume_manager.download_image.return_value = ('image_file', None)
    volume_manager.init_image()
    volume_manager.download_image.assert_called_with(fake_resource_url)
    qemu_img_mock.resize.assert_called_with('image_file', volume_pb.available_size)
    volume_manager.validate_image_size.assert_called_with('image_file')
    volume_manager.create_image_symlinks.assert_called_with('image_file', delta_file=None)

    # case: init with resource url (image and delta file has been downloaded)
    volume_pb.resource_url = fake_resource_url
    volume_manager.download_image.return_value = ('image_file', 'delta_file')
    with pytest.raises(volume_manager.DownloadingImageError) as e:
        volume_manager.init_image()
    assert e.value.message == "Downloading resource_url: any_url contain more then one image file for RAW image"


def test_init_image_main_delta(volume_pb, volume_manager, qemu_img_mock):
    qemu_img_mock.resize.return_value = (True, '')
    qemu_img_mock.rebase.return_value = (True, '')

    volume_manager.create_image = mock.Mock()
    volume_manager.download_image = mock.Mock()
    volume_manager.validate_image_size = mock.Mock()
    volume_manager.create_image_symlinks = mock.Mock()

    fake_resource_url = 'any_url'
    volume_pb.available_size = 100
    volume_pb.is_main = True
    volume_pb.image_type = vmagent_pb2.VMVolume.DELTA
    volume_pb.resource_url = ''

    # case: init empty
    with pytest.raises(volume_manager.ImageError) as e:
        volume_manager.init_image()
    assert e.value.message == "Main volume can't be empty (resource_url required)"

    # case: init with resource url (image file has been downloaded)
    volume_pb.resource_url = fake_resource_url
    volume_manager.download_image.return_value = ('image_file', None)
    volume_manager.create_image.return_value = 'delta_file'
    volume_manager.init_image()
    volume_manager.download_image.assert_called_with(fake_resource_url)
    volume_manager.create_image.assert_called_with(backing_file='image_file')

    volume_manager.validate_image_size.assert_not_called()
    volume_manager.create_image_symlinks.assert_called_with('image_file', delta_file='delta_file')

    # case: init with resource url (image and delta file has been downloaded)
    qemu_img_mock.reset_mock()
    volume_manager.validate_image_size.reset_mock()
    volume_manager.create_image_symlinks.reset_mock()
    image_size = 3
    volume_pb.resource_url = fake_resource_url
    volume_manager.download_image.return_value = ('image_file', 'delta_file')
    with mock.patch('infra.qyp.vmagent.src.helpers.get_image_size') as get_image_size_mock:
        get_image_size_mock.return_value = image_size
        volume_manager.init_image()

    get_image_size_mock.assert_called_with('image_file')
    qemu_img_mock.rebase.assert_called_with('delta_file', 'image_file')
    qemu_img_mock.resize.assert_called_with('delta_file', volume_pb.available_size - image_size)
    volume_manager.validate_image_size.assert_called_with('image_file', 'delta_file')
    volume_manager.create_image_symlinks.assert_called_with('image_file', delta_file='delta_file')


def test_init_image_extra_raw(volume_pb, volume_manager, qemu_img_mock):
    qemu_img_mock.resize.return_value = (True, '')
    qemu_img_mock.rebase.return_value = (True, '')

    volume_manager.create_image = mock.Mock()
    volume_manager.download_image = mock.Mock()
    volume_manager.validate_image_size = mock.Mock()
    volume_manager.create_image_symlinks = mock.Mock()

    fake_resource_url = 'any_url'
    volume_pb.available_size = 100

    volume_pb.is_main = False
    volume_pb.image_type = vmagent_pb2.VMVolume.RAW
    volume_pb.resource_url = ''

    # case: init empty
    volume_manager.create_image.return_value = 'image_file'
    volume_manager.init_image()

    volume_manager.create_image.assert_called_with()
    volume_manager.validate_image_size.assert_not_called()
    volume_manager.create_image_symlinks.assert_called_with('image_file', delta_file=None)

    # case: init with resource url (downloaded image file)
    volume_pb.resource_url = fake_resource_url
    volume_manager.download_image.return_value = ('image_file', None)
    volume_manager.init_image()
    volume_manager.download_image.assert_called_with(fake_resource_url)
    qemu_img_mock.resize.assert_called_with('image_file', volume_pb.available_size)
    volume_manager.validate_image_size.assert_called_with('image_file')
    volume_manager.create_image_symlinks.assert_called_with('image_file', delta_file=None)

    # case: init with resource url (downloaded image and delta file)
    volume_pb.resource_url = fake_resource_url
    volume_manager.download_image.return_value = ('image_file', 'delta_file')
    with pytest.raises(volume_manager.ImageError) as e:
        volume_manager.init_image()
    assert e.value.message == "Downloading resource_url: any_url contain more then one image file for RAW image"


def test_init_image_extra_delta(volume_pb, volume_manager, qemu_img_mock):
    qemu_img_mock.resize.return_value = (True, '')
    qemu_img_mock.rebase.return_value = (True, '')

    volume_manager.create_image = mock.Mock()
    volume_manager.download_image = mock.Mock()
    volume_manager.validate_image_size = mock.Mock()
    volume_manager.create_image_symlinks = mock.Mock()

    fake_resource_url = 'any_url'
    volume_pb.available_size = 100
    volume_pb.is_main = False
    volume_pb.image_type = vmagent_pb2.VMVolume.DELTA
    volume_pb.resource_url = ''

    # case: init empty
    volume_manager.download_image.side_effect = volume_manager.DownloadingImageError()
    with pytest.raises(volume_manager.ImageError) as e:
        volume_manager.init_image()
    assert e.value.message == "image file required for image_type == DELTA"

    # case: init with resource url (downloaded image file)
    volume_manager.download_image.side_effect = None
    volume_pb.resource_url = fake_resource_url
    volume_manager.download_image.return_value = ('image_file', None)
    volume_manager.create_image.return_value = 'delta_file'
    volume_manager.init_image()
    volume_manager.download_image.assert_called_with(fake_resource_url)
    volume_manager.create_image.assert_called_with(backing_file='image_file')

    volume_manager.validate_image_size.assert_not_called()
    volume_manager.create_image_symlinks.assert_called_with('image_file', delta_file='delta_file')

    # case: init with resource url (downloaded image and delta file)
    image_size = 3
    volume_pb.resource_url = fake_resource_url
    volume_manager.download_image.return_value = ('image_file', 'delta_file')
    with mock.patch('infra.qyp.vmagent.src.helpers.get_image_size') as get_image_size_mock:
        get_image_size_mock.return_value = image_size
        volume_manager.init_image()

    get_image_size_mock.assert_called_with('image_file')
    qemu_img_mock.rebase.assert_called_with('delta_file', 'image_file')
    qemu_img_mock.resize.assert_called_with('delta_file', volume_pb.available_size - image_size)
    volume_manager.validate_image_size.assert_called_with('image_file', 'delta_file')
    volume_manager.create_image_symlinks.assert_called_with('image_file', delta_file='delta_file')
