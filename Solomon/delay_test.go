package uhttp

import (
	"github.com/stretchr/testify/assert"
	"math/rand"
	"testing"
	"time"
)

func TestNextDelay(t *testing.T) {
	rand.Seed(17)

	assert.Equal(t, time.Second, nextDelay(0, 5))
	assert.Equal(t, time.Duration(2127073290), nextDelay(1, 5)) // 2.1s
	assert.Equal(t, time.Duration(3505253061), nextDelay(2, 5)) // 3.5s
	assert.Equal(t, time.Duration(3904351075), nextDelay(3, 5)) // 3.9s
	assert.Equal(t, time.Duration(5341407592), nextDelay(4, 5)) // 5.3s
	assert.Equal(t, time.Duration(8080180107), nextDelay(5, 5)) // 8.0s
}
