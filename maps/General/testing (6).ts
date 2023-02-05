import Config from './interface';
import production from './production';

const testing: Config = {
    ...production,
    hosts: {
        ...production.hosts,
        environment: 'testing'
    },
    tvm: {
        ...production.tvm,
        clientId: 2019888,
        dsts: {
            blackbox: '223',
            moira: '2012240'
        }
    },
    bitbucket: {
        ...production.bitbucket,
        projectKey: '~artemsavossin',
        repositorySlug: 'navi_s3_test',
        targetProjectKey: '~artemsavossin',
        targetRepositorySlug: 'navi_s3_test',
        targetBranch: 'dev'
    }
};

export default testing;
