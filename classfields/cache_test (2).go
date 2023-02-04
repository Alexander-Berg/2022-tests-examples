package tvmauth

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestSrvTicketCache_Get(t *testing.T) {
	cache := newSrvCache()
	cache.Set(1, 2, "xyz")
	assert.Equal(t, "xyz", cache.Get(1, 2))
}
