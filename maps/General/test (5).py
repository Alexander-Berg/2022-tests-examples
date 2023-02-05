import os
os.environ['TF_CPP_MIN_LOG_LEVEL']='2'
import tensorflow as tf
import numpy as np
import os.path
import models
import moving_mnist as mm

print(tf.__version__)

SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))

INPUT_SEQ_LENGTH  = 10
OUTPUT_SEQ_LENGTH = 10

BATCH_SIZE = 16
OUT_FOLDER  = os.path.abspath("./model-data/test_do0.5all/")

EPOCH_INDEX = 63

DROP_OUT = 0.5

PEEPHOLE_ENABLED = True
BATCHNORM_ENABLED = True
CONDITIONAL_MODEL = False

_, valid_data, _ = mm.load_data(SCRIPT_DIR + "/data/")

data_X, data_Y = np.split(mm.encode_data(valid_data), [INPUT_SEQ_LENGTH, ], 1)
X = tf.placeholder(tf.float32, [None, data_X.shape[1], data_X.shape[2], data_X.shape[3], data_X.shape[4]] , name = "X")
keep_prob = tf.placeholder(tf.float32)
is_training = tf.placeholder(tf.bool)
model = models.get_model(CONDITIONAL_MODEL, 3, BATCHNORM_ENABLED, PEEPHOLE_ENABLED, DROP_OUT < 1.0)
output = tf.sigmoid(model(X, keep_prob, is_training, OUTPUT_SEQ_LENGTH))

saver = tf.train.Saver()
with tf.Session() as sess:
    sess.run(tf.global_variables_initializer())
    model_path = os.path.join(OUT_FOLDER, 'model-%d' % EPOCH_INDEX)
    saver.restore(sess, model_path)

    sess.graph.finalize()

    print("Epoch: ", EPOCH_INDEX)
    ######################################################################
    data_X = data_X[0 : BATCH_SIZE, :, :, :, :]
    data_Y = data_Y[0 : BATCH_SIZE, :, :, :, :]
    pred = sess.run(output, feed_dict={X: data_X, keep_prob: 1.0, is_training: False})
    print(pred.shape)

    dataset_gt = mm.decode_data(np.concatenate((data_X, data_Y), axis = 1))
    folder_path = "{}gt".format(OUT_FOLDER)
    try:
        os.mkdir(folder_path)
    except:
        pass
    mm.save_data(dataset_gt, folder_path)

    dataset_pred = mm.decode_data(np.concatenate((data_X, pred), axis = 1))
    folder_path = "{}train_result{}".format(OUT_FOLDER, EPOCH_INDEX)
    try:
        os.mkdir(folder_path)
    except:
        pass
    mm.save_data(dataset_pred, folder_path + "")

