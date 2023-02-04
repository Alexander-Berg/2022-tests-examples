package errors

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestWrapNil(t *testing.T) {
	ret := Wrap(nil, "test")
	assert.NoError(t, ret)
}

func TestWrapErr(t *testing.T) {
	ret := Wrap(errors.New("test"), "wrap")
	assert.Error(t, ret)
	assert.EqualError(t, ret, "wrap: test")
}

func TestWrapfNil(t *testing.T) {
	ret := Wrapf(nil, "test %v", 1)
	assert.NoError(t, ret)
}

func TestWrapfErr(t *testing.T) {
	ret := Wrapf(errors.New("test"), "wrap %v", 1)
	assert.Error(t, ret)
	assert.EqualError(t, ret, "wrap 1: test")
}
