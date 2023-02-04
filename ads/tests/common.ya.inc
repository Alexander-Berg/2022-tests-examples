OWNER(stepych)

SIZE(LARGE)

TAG(
  ya:fat
  ya:yt
)

PEERDIR(
  build/platform/cuda
  ads/quality/sis/sis/cuda
  ads/quality/sis/sis/cuda/standalone
  ads/quality/sis/sis/data
  ads/quality/sis/sis/tensorrt
)

SRCDIR(ads/quality/sis/tests)
SRCS(
  compiler_test_setup.cpp  
  mma_test_data.cpp  
  test_nothing_stub.cpp
  test_ragged.cpp
  test_reflection.cpp
  test_graph_transformation.cpp
  test_attention_layer.cpp
  test_fully_connected_layer.cpp  
  test_inference.cpp  
  test_sparse_embeddings.cpp  
  integration_graph.cpp  
  test_constant_layer.cpp  
  test_fusion.cpp  
  test_integration.cpp  
  test_cuda_program.cpp
  test_gather_layer.cpp  
  test_layer_norm_layer.cpp  
  test_scale_layer.cpp  
  test_activation_layer.cpp  
  test_dimensions.cpp  
  test_identity_layer.cpp
  test_quantize_layer.cpp
  test_matrix_multiply.cpp  
  test_softmax_layer.cpp
  test_multi_rags.cpp
  test_slice_layer.cpp
  test_unary_layer.cpp
  test_element_wise_layer.cpp
  test_calibration.cpp
  test_concatenation_layer.cpp
  test_reshape_layer.cpp
  test_softmax_with_lens.cpp
  test_dense_attention.cpp
  test_transpose_layer.cpp
  test_inner_ragged.cpp
)

