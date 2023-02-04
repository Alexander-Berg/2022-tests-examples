# coding: utf-8

__author__ = 'a-vasin'

import btestlib.reporter as reporter
import btestlib.utils as utils

# Кладем и достаем строку
utils.s3storage().set_string_value("test_key", "test_value")
reporter.log(utils.s3storage().get_string_value("test_key"))

# Кладем и достаем файл
utils.s3storage().set_file_value("test_file_key", utils.project_file("scripts/examples/s3storage_examples.py"))
utils.s3storage().get_file_value("test_file_key", utils.project_file("scripts/examples/s3storage_examples_copy.py"))

# Удаляем ключ и проверяем его наличие
utils.s3storage().delete_key("test_key")
reporter.log(utils.s3storage().is_present("test_key"))

# Создаем новый бакет
custom_bucket = utils.S3Storage(bucket_name="test-bucket")

utils.s3storage().set_string_value("test_key", "value_1")
custom_bucket.set_string_value("test_key", "value_2")

reporter.log(utils.s3storage().get_string_value("test_key"))
reporter.log(custom_bucket.get_string_value("test_key"))
