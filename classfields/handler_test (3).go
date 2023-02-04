package driver

import (
	"testing"

	"github.com/docker/go-plugins-helpers/ipam"
	"github.com/docker/libnetwork/ipamapi"
	"github.com/docker/libnetwork/netlabel"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestRequestAddress(t *testing.T) {
	h, err := NewVertisIPAM(t.TempDir() + "/db")
	require.NoError(t, err)
	pool, err := h.RequestPool(&ipam.RequestPoolRequest{
		Pool: "2a02:42::/112",
		V6:   true,
	})
	require.NoError(t, err)
	assert.Equal(t, "2a02:42::/112", pool.Pool)
	assert.NotEmpty(t, pool.PoolID)

	gwResp, err := h.RequestAddress(&ipam.RequestAddressRequest{
		PoolID: pool.PoolID,
		Options: map[string]string{
			ipamapi.RequestAddressType: netlabel.Gateway,
		},
	})
	require.NoError(t, err)
	assert.Equal(t, "2a02:42::1/112", gwResp.Address)

	addrResp, err := h.RequestAddress(&ipam.RequestAddressRequest{
		PoolID: pool.PoolID,
	})
	require.NoError(t, err)
	assert.Equal(t, "2a02:42::2/112", addrResp.Address)

	addrResp2, err := h.RequestAddress(&ipam.RequestAddressRequest{
		PoolID: pool.PoolID,
	})
	require.NoError(t, err)
	assert.Equal(t, "2a02:42::3/112", addrResp2.Address)

	require.NoError(t, h.ReleaseAddress(&ipam.ReleaseAddressRequest{PoolID: pool.PoolID, Address: "2a02:42::2"}))
	require.NoError(t, h.ReleaseAddress(&ipam.ReleaseAddressRequest{PoolID: pool.PoolID, Address: "2a02:42::3"}))
	require.NoError(t, h.ReleasePool(&ipam.ReleasePoolRequest{PoolID: pool.PoolID}))
}

func TestRequestAddress_Overflow(t *testing.T) {
	h, err := NewVertisIPAM(t.TempDir() + "/db")
	require.NoError(t, err)
	pool, err := h.RequestPool(&ipam.RequestPoolRequest{
		Pool: "2a02:42::/124", // 15 IPs subnet
		V6:   true,
	})
	require.NoError(t, err)

	req := &ipam.RequestAddressRequest{PoolID: pool.PoolID}
	for i := 0; i < 15; i++ {
		_, err := h.RequestAddress(req)
		require.NoError(t, err, "addr %d allocate fail", i)
	}
	// should fail because no free addresses
	_, err = h.RequestAddress(req)
	require.Error(t, err)

	err = h.ReleaseAddress(&ipam.ReleaseAddressRequest{PoolID: pool.PoolID, Address: "2a02:42::5"})
	require.NoError(t, err)

	addrInfo, err := h.RequestAddress(req)
	require.NoError(t, err)
	assert.Equal(t, "2a02:42::5/124", addrInfo.Address)
}

func TestRequestAddress_Sequence(t *testing.T) {
	h, err := NewVertisIPAM(t.TempDir() + "/db")
	require.NoError(t, err)
	pool, err := h.RequestPool(&ipam.RequestPoolRequest{
		Pool: "2a02:42::/124", // 15 IPs subnet
		V6:   true,
	})
	require.NoError(t, err)

	req := &ipam.RequestAddressRequest{PoolID: pool.PoolID}
	for i := 0; i < 5; i++ {
		_, err := h.RequestAddress(req)
		require.NoError(t, err, "addr %d allocate fail", i)
	}

	err = h.ReleaseAddress(&ipam.ReleaseAddressRequest{PoolID: pool.PoolID, Address: "2a02:42::3"})
	require.NoError(t, err)
	err = h.ReleaseAddress(&ipam.ReleaseAddressRequest{PoolID: pool.PoolID, Address: "2a02:42::5"})
	require.NoError(t, err)

	// checking that releasing IPs does not break sequence
	addrInfo, err := h.RequestAddress(req)
	require.NoError(t, err)
	assert.Equal(t, "2a02:42::6/124", addrInfo.Address)
}

func TestRequestAddress_Restart(t *testing.T) {
	dbPath := t.TempDir() + "/db"

	h, err := NewVertisIPAM(dbPath)
	require.NoError(t, err)
	pool, err := h.RequestPool(&ipam.RequestPoolRequest{
		Pool: "2a02:42::/64", // 15 IPs subnet
		V6:   true,
	})
	require.NoError(t, err)

	req := &ipam.RequestAddressRequest{PoolID: pool.PoolID}
	a1, err := h.RequestAddress(req)
	require.NoError(t, err)
	assert.Equal(t, "2a02:42::1/64", a1.Address)

	// test that recreating plugins picks up database data
	h2, err := NewVertisIPAM(dbPath)
	require.NoError(t, err)
	a2, err := h2.RequestAddress(req)
	require.NoError(t, err)
	assert.Equal(t, "2a02:42::2/64", a2.Address)
}

func TestRelease_NotExists(t *testing.T) {
	h, err := NewVertisIPAM(t.TempDir() + "/db")
	require.NoError(t, err)
	err = h.ReleasePool(&ipam.ReleasePoolRequest{PoolID: "non-existent"})
	require.Error(t, err)

	err = h.ReleaseAddress(&ipam.ReleaseAddressRequest{PoolID: "ne", Address: "2a02:42::1/112"})
	require.Error(t, err)
}
