package rw

import (
	"encoding/base64"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestParsePubKey(t *testing.T) {
	key := base64Decode(`MIIBBAKCAQBwsRd4frsVARIVSfj_vCdfvA3Q9SsGhSybdBDhbm8L6rPqxdoSNLCdNXzDWj7Ppf0o8uWHMxC-5Lfw0I18ri68nhm9-ndixcnbn6ti1uetgkc28eiEP6Q8ILD_JmkynbUl1aKDNAa5XsK2vFSEX402uydRomsTn46kRY23hfqcIi0ohh5VxIrpclRsRZus0JFu-RJzhqTbKYV4y4dglWPGHh5BuTv9k_Oh0_Ra8Xp5Rith5vjaKZUQ5Hyh9UtBYTkNWdvXP9OpmbiLVeRLuMzBm4HEFHDwMZ1h6LSVP-wB_spJPaMLTn3Q3JIHe-wGBYRWzU51RRYDqv4O_H12w5C1`)
	_, err := ParsePubKey(key)
	require.NoError(t, err)

	_, err = ParsePubKey(nil)
	require.Equal(t, err, ErrEmptyKey)

	_, err = ParsePubKey([]byte("gibberish"))
	require.Equal(t, err, ErrMalformedKey)
}

func TestRWPubKey_CheckSign(t *testing.T) {
	// test data from https://a.yandex-team.ru/arc_vcs/library/cpp/tvmauth/src/rw/ut/rw_ut.cpp?rev=r9017363#L14
	privKey, err := ParsePrivateKey(base64Decode(`MIIEmwKCAQBwsRd4frsVARIVSfj_vCdfvA3Q9SsGhSybdBDhbm8L6rPqxdoSNLCdNXzDWj7Ppf0o8uWHMxC-5Lfw0I18ri68nhm9-ndixcnbn6ti1uetgkc28eiEP6Q8ILD_JmkynbUl1aKDNAa5XsK2vFSEX402uydRomsTn46kRY23hfqcIi0ohh5VxIrpclRsRZus0JFu-RJzhqTbKYV4y4dglWPGHh5BuTv9k_Oh0_Ra8Xp5Rith5vjaKZUQ5Hyh9UtBYTkNWdvXP9OpmbiLVeRLuMzBm4HEFHDwMZ1h6LSVP-wB_spJPaMLTn3Q3JIHe-wGBYRWzU51RRYDqv4O_H12w5C1AoGBALAwCQ7fdAPG1lGclL7iWFjUofwPCFwPyDjicDT_MRRu6_Ta4GjqOGO9zuOp0o_ePgvR-7nA0fbaspM4LZNrPZwmoYBCJMtKXetg68ylu2DO-RRSN2SSh1AIZSA_8UTABk69bPzNL31j4PyZWxrgZ3zP9uZvzggveuKt5ZhCMoB7AoGBAKO9oC2AZjLdh2RaEFotTL_dY6lVcm38VA6PnigB8gB_TMuSrd4xtRw5BxvHpOCnBcUAJE0dN4_DDe5mrotKYMD2_3_lcq9PaLZadrPDCSDL89wtoVxNQNAJTqFjBFXYNu4Ze63lrsqg45TF5XmVRemyBHzXw3erd0pJaeoUDaSPAoGAJhGoHx_nVw8sDoLzeRkOJ1_6-uh_wVmVr6407_LPjrrySEq-GiYu43M3-QDp8J_J9e3S1Rpm4nQX2bEf5Gx9n4wKz7Hp0cwkOqBOWhvrAu6YLpv59wslEtkx0LYcJy6yQk5mpU8l29rPO7b50NyLnfnE2za-9DyK038FKlr5VgICgYAUd7QFsAzGW7Dsi0ILRamX-6x1Kq5Nv4qB0fPFAD5AD-mZclW7xjajhyDjePScFOC4oASJo6bx-GG9zNXRaUwYHt_v_K5V6e0Wy07WeGEkGX57hbQriagaASnULGCKuwbdwy91vLXZVBxymLyvMqi9NkCPmvhu9W7pSS09QoG0kgKBgBYGASHb7oB42sozkpfcSwsalD-B4QuB-QccTgaf5iKN3X6bXA0dRwx3udx1OlH7x8F6P3c4Gj7bVlJnBbJtZ7OE1DAIRJlpS71sHXmUt2wZ3yKKRuySUOoBDKQH_iiYAMnXrZ-Zpe-sfB-TK2NcDO-Z_tzN-cEF71xVvLMIRlAPAoGAdeikZPh1O57RxnVY72asiMRZheMBhK-9uSNPyYEZv3bUnIjg4XdMYStF2yTHNu014XvkDSQTe-drv2BDs9ExKplM4xFOtDtPQQ3mMB3GoK1qVhM_9n1QEElreurMicahkalnPo6tU4Z6PFL7PTpjRnCN67lJp0J0fxNDL13YSagCgYBA9VJrMtPjzcAx5ZCIYJjrYUPqEG_ttQN2RJIHN3MVpdpLAMIgX3tnlfyLwQFVKK45D1JgFa_1HHcxTWGtdIX4nsIjPWt-cWCCCkkw9rM5_Iqcb-YLSood6IP2OK0w0XLD1STnFRy_BRwdjPbGOYmp6YrJDZAlajDkFSdRvsz9Vg==`))
	require.NoError(t, err)
	pubKey, err := ParsePubKey(base64Decode(`MIIBBAKCAQBwsRd4frsVARIVSfj_vCdfvA3Q9SsGhSybdBDhbm8L6rPqxdoSNLCdNXzDWj7Ppf0o8uWHMxC-5Lfw0I18ri68nhm9-ndixcnbn6ti1uetgkc28eiEP6Q8ILD_JmkynbUl1aKDNAa5XsK2vFSEX402uydRomsTn46kRY23hfqcIi0ohh5VxIrpclRsRZus0JFu-RJzhqTbKYV4y4dglWPGHh5BuTv9k_Oh0_Ra8Xp5Rith5vjaKZUQ5Hyh9UtBYTkNWdvXP9OpmbiLVeRLuMzBm4HEFHDwMZ1h6LSVP-wB_spJPaMLTn3Q3JIHe-wGBYRWzU51RRYDqv4O_H12w5C1`))
	require.NoError(t, err)
	data := []byte("my magic data")

	assert.True(t, pubKey.CheckSign(data, privKey.Sign(data)))
	assert.False(t, pubKey.CheckSign([]byte("~~~~~"+string(data)), privKey.Sign(data)))
	assert.False(t, pubKey.CheckSign(data, []byte("~~~~~"+string(privKey.Sign(data)))))

	assert.True(t, pubKey.CheckSign(data,
		base64Decode("EC5hZunmK3hOJZeov_XlNIXcwj5EsgX94lMd-tQJTNUO4NR6bCO7qQkKjEeFJmI2QFYXGY-iSf9WeMJ_brECAMyYAix-L8sZqcMPXD945QgkPsNQKyC0DX9FkgfSh6ZKkA-UvFSHrkn3QbeE9omk3-yXpqR-M8DlVqmp3mwdYlYRq0NdfTaD3AMXVA4aZTbW3OmhJoLJ8AxJ3w1oG5q_lk8dpW9vvqfIzsfPABme6sY5XyPmsjYaRDf9z4ZJgR-wTkG06_N_YzIklS5T2s_4FUKLz5gLMhsnVlNUpgZyRN9sXTAn9-zMJnCwAC8WRgykWnljPGDDJCjk-Xwsg7AOLQ==")))
	assert.True(t, pubKey.CheckSign(data,
		base64Decode("JbHSn1QEQeOEvzyt-LpawbQv4vPEEE05bWhjB2-MkoV-tyq9FykSqGqhP3ZFc1_FPrqguwEYrHibI2l5w3q8wnI1fcyRUoNuJxmBSzf2f_Uzn9ZoUSc7D9pTGSvK_hhZoL4YMc_VfbdEdnDuvHZNlZyaDPH9EbmUqyXjnXTEwRoK0fAU1rhlHvSZvnp0ctVBWSkaQsaU8dJTKDBtIQVP1D5Py2pKB2NBF_Ytz2thWt7iLjbTyjtis6DC-JKwjFBqv6nQf42sKalHQqWFuIvBCIfNUswEw4_sGfwWVSBBmFplf7FmD7sN8znUahYUPGCe1uFNly6WwpPJsm8VtiU80g==")))
	assert.True(t, pubKey.CheckSign(data,
		base64Decode("FeMZtDP-yuoNqK2HYw3JxTV9v7p8IoQEuRMtuHddafh4bq1ZOeEqg7g7Su6M3iq_kN9DZ_fVhuhuVcbZmNYPIvJ8oL5DE80KI3d1Qbs9mS8_X4Oq2TJpZgNfFG-z_LPRZSNRP9Q8sQhlAoSZHOSZkBFcYj1EuqEp6nSSSbX8Ji4Se-TfhIh3YFQkr-Ivk_3NmSXhDXUaW7CHo2rVm58QJ2cgSEuxzBH-Q8E8tGDCEmk4p3_iot9XY8RRN-_j0yi15etmXCUIKFbpDogtHdT8CyAEVHMYvsLqkLux9pzy3RdvNQmoPjol3wIm-H0wMtF_pMw4G2QLNev6he6xWeckxw==")))
}

func TestGenerateKey(t *testing.T) {
	require.Nil(t, GenerateKey(-1), "wrong key size should produce empty result")
	key := GenerateKey(2048)
	require.NotNil(t, key, "key generate error")
	sig := key.Sign([]byte("test-message"))
	assert.True(t, len(sig) > 0, "sign length check failed")
	assert.True(t, key.PublicKey.CheckSign([]byte("test-message"), sig))
	assert.False(t, key.PublicKey.CheckSign([]byte("wrong-message"), sig))
}

func base64Decode(in string) []byte {
	v, err := base64.URLEncoding.DecodeString(in)
	if err != nil {
		return nil
	}
	return v
}
