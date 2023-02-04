package parser

import (
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
)

const yml = `
STR_PARAM: string params
INT_PARAM: 1234
BOOL_PARAM: false
FLOAT_PARAM: 1.5
DURATION_PARAM: 5h
DOUBLE_PARAM: double 1
DOUBLE_PARAM: double 2
`

func TestParse(t *testing.T) {

	srv := NewService(test.NewLogger(t))
	params, err := srv.ParseByte([]byte(yml))
	test.Check(t, err)
	assert.Equal(t, params["STR_PARAM"], "string params")
	assert.Equal(t, params["INT_PARAM"], "1234")
	assert.Equal(t, params["BOOL_PARAM"], "false")
	assert.Equal(t, params["FLOAT_PARAM"], "1.5")
	assert.Equal(t, params["DURATION_PARAM"], "5h")
	assert.Equal(t, params["DOUBLE_PARAM"], "double 2")
}
