package errors

import (
	"errors"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestNewInternalError(t *testing.T) {
	err := errors.New("internal error: test error. Please contact vertis-duty")
	assert.Equal(t, err.Error(), NewInternalError(errors.New("test error")).Error())
}
