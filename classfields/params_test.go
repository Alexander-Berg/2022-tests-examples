package param

import (
	"github.com/stretchr/testify/assert"
	"log"
	"testing"
)

func TestInitParams(t *testing.T) {

	params := Params()
	log.Printf("Params: %v", params)
	assert.True(t, len(params) > 0)

	param, ok := ParamsByPath()["datacenters.sas.count"]
	assert.True(t, ok)
	assert.Equal(t, param, Sas)

	param, ok = ParamsByPath()["resources.cpu"]
	assert.True(t, ok)
	assert.Equal(t, param, CPU)

}
