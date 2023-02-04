package hosts

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestParseEmptyProtoText(t *testing.T) {
	clusters := ParseClusterListProtoText(``)
	assert.Equal(t, 0, len(clusters))
}

func TestParseSingleClusterFromProtoText(t *testing.T) {
	clusters := ParseClusterListProtoText(`
		pre {
			replicas { z2_config_id: "SOLOMON_PRE_FETCHER", host_pattern: "(man-)?\\d{3}", dc: "man" }
		}
	`)

	assert.Equal(t, 1, len(clusters))
	assert.Equal(t, EnvPre, clusters[0].Env)
	assert.Equal(t, 1, len(clusters[0].Replicas))

	replica := clusters[0].Replicas[0]
	assert.Equal(t, "SOLOMON_PRE_FETCHER", replica.Z2ConfigID)
	assert.Equal(t, "(man-)?\\d{3}", replica.HostPattern.String())
	assert.Equal(t, 1, len(replica.Dcs))
	assert.Equal(t, DcMan, replica.Dcs[0])
}

func TestParseMultipleClustersFromProtoText(t *testing.T) {
	clusters := ParseClusterListProtoText(`
		test {
			replicas { z2_config_id: "SOLOMON_FAKE", host_pattern: "(sas|vla|myt)-\\d{2}", dc: "sas,vla,myt" }
		}
		pre {
			replicas { z2_config_id: "SOLOMON_PRE_STOCKPILE", host_pattern: "(man-)?\\d{3}", dc: "man" }
		}
		prod {
			replicas { z2_config_id: "SOLOMON_PROD_STOCKPILE_SAS", host_pattern: "sas-\\d{3}", dc: "sas" }
			replicas { z2_config_id: "SOLOMON_PROD_STOCKPILE_VLA", host_pattern: "vla-\\d{3}", dc: "vla" }
		}
	`)

	assert.Equal(t, 3, len(clusters))
	{
		testCluster := clusters[0]
		assert.Equal(t, EnvTest, testCluster.Env)
		assert.Equal(t, 1, len(testCluster.Replicas))

		z2Cfg := testCluster.Replicas[0]
		assert.Equal(t, "SOLOMON_FAKE", z2Cfg.Z2ConfigID)
		assert.Equal(t, "(sas|vla|myt)-\\d{2}", z2Cfg.HostPattern.String())
		assert.Equal(t, 3, len(z2Cfg.Dcs))
		assert.Equal(t, DcSas, z2Cfg.Dcs[0])
		assert.Equal(t, DcVla, z2Cfg.Dcs[1])
		assert.Equal(t, DcMyt, z2Cfg.Dcs[2])
	}
	{
		preCluster := clusters[1]
		assert.Equal(t, EnvPre, preCluster.Env)
		assert.Equal(t, 1, len(preCluster.Replicas))

		z2Cfg := preCluster.Replicas[0]
		assert.Equal(t, "SOLOMON_PRE_STOCKPILE", z2Cfg.Z2ConfigID)
		assert.Equal(t, "(man-)?\\d{3}", z2Cfg.HostPattern.String())
		assert.Equal(t, 1, len(z2Cfg.Dcs))
		assert.Equal(t, DcMan, z2Cfg.Dcs[0])
	}
	{
		prodCluster := clusters[2]
		assert.Equal(t, EnvProd, prodCluster.Env)
		assert.Equal(t, 2, len(prodCluster.Replicas))

		z2CfgSas := prodCluster.Replicas[0]
		assert.Equal(t, "SOLOMON_PROD_STOCKPILE_SAS", z2CfgSas.Z2ConfigID)
		assert.Equal(t, "sas-\\d{3}", z2CfgSas.HostPattern.String())
		assert.Equal(t, 1, len(z2CfgSas.Dcs))
		assert.Equal(t, DcSas, z2CfgSas.Dcs[0])

		z2CfgVla := prodCluster.Replicas[1]
		assert.Equal(t, "SOLOMON_PROD_STOCKPILE_VLA", z2CfgVla.Z2ConfigID)
		assert.Equal(t, "vla-\\d{3}", z2CfgVla.HostPattern.String())
		assert.Equal(t, 1, len(z2CfgVla.Dcs))
		assert.Equal(t, DcVla, z2CfgVla.Dcs[0])
	}
}
