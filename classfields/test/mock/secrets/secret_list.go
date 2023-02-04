package secrets

import proto "github.com/YandexClassifieds/shiva/pb/ss/access"

type secretList []*proto.EnvSecret

func (l secretList) Len() int {
	return len(l)
}

func (l secretList) Less(i, j int) bool {
	switch {
	case l[i].EnvKey != l[j].EnvKey:
		return l[i].EnvKey < l[j].EnvKey
	case l[i].SecretId != l[j].SecretId:
		return l[i].SecretId < l[j].SecretId
	case l[i].VersionId != l[j].VersionId:
		return l[i].VersionId < l[j].VersionId
	case l[i].SecretKey != l[j].SecretKey:
		return l[i].SecretKey < l[j].SecretKey
	}
	return false
}

func (l secretList) Swap(i, j int) {
	l[i], l[j] = l[j], l[i]
}
