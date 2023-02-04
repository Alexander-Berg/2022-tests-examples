IF(NOT AUTOCHECK)
  RECURSE(
    vm-layer
    vm-image
    vm-layer-5.4.134-19
    vm-image-5.4.134-19
    vm-layer-5.4.187-35.2
    vm-image-5.4.187-35.2
  )
ENDIF()

RECURSE(
  release
)
