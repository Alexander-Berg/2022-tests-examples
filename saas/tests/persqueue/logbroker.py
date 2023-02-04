import logging
import time
import uuid
import json
import urllib2
import os


def get_sandbox_task_id():
    unknown_value = 'unknown'
    gsid = os.environ.get('GSID', unknown_value)
    logging.info('env variable GSID=%s' % gsid)
    id = gsid.split(' ').pop().split(':').pop()
    return id if id.isalnum() else unknown_value


class Api(object):
    def __init__(self, owner, user, token):
        self._owner = owner
        self._user = user
        self.__balancer = 'logbroker-prestable.yandex.net'
        self.__default_cluster = 'man'
        self.__command_url = '{host}:{port}'.format(host=self.get_server(), port=3663)
        self.__commands = []
        self.__get_token(token)

    def __get_token(self, token):
        if token:
            self.__token = token
            return
        self.__token = os.environ.get('PQ_API_TOKEN')
        if not self.__token:
            token_path = os.path.join(os.getenv("HOME"), '.saas/pq_api_token')
            try:
                with open(token_path) as f:
                    self.__token = f.read()
            except Exception as e:
                logging.error(e)
        if not self.__token:
            raise Exception('Unable to get pq api token')

    def get_server(self, cluster=None):
        if cluster is None:
            cluster = self.__default_cluster
        return '{cluster}.{balancer}'.format(cluster=cluster, balancer=self.__balancer)

    def __try_request(self, url, json_data=None, retries=3, wait_seconds=1, timeout=60):
        if not url.startswith('http://') and not url.startswith('https://'):
            url = 'http://' + url
        for retrie in range(retries):
            try:
                request = urllib2.Request(url)
                if json_data:
                    request.add_header('Content-Type', 'application/json')
                    result = urllib2.urlopen(request, json.dumps(json_data), timeout=timeout)
                else:
                    result = urllib2.urlopen(request, timeout=timeout)
            except urllib2.HTTPError, e:
                logging.error(url + ' ' + str(e))
                logging.error(e.read())
            except urllib2.URLError, e:
                logging.error(url + ' ' + str(e))
            except Exception, e:
                logging.error(url + ' ' + str(e))
            else:
                return result.read()
            time.sleep(wait_seconds)
        return None

    def get_topic_name(self, ident, logtype, cluster=None):
        topic = '{ident}--{logtype}'.format(ident=ident, logtype=logtype)
        if cluster:
            topic = '{cluster}--{topic}'.format(cluster=cluster, topic=topic)
        return topic

    def __get_topic_key(self, origin_cluster, cluster, topic):
        return {
            'origin-cluster': origin_cluster,
            'cluster': cluster,
            'topic-id': topic
        }

    def __get_command_create_topic(self, topic_key, partitions):
        return {
            'command': 'create',
            'entity': 'topic',
            'parent': 'default',
            'key': topic_key,
            'config': {
                'partitions': partitions,
                'responsible-for-data': 'saas@',
                "size-per-minute": '1'
            }
        }

    def __get_command_delete_topic(self, topic_key):
        return {
            'command': 'delete',
            'entity': 'topic',
            'key': topic_key
        }

    def __get_command_change_acl(self, command, cluster, consumer, topic_key):
        return {
            'command': command,
            'entity': 'consumer-acl',
            'key': {
                'consumer': {
                    'cluster': cluster,
                    'name': consumer
                },
                'topic': topic_key
            },
            'config': {},
            'parent': 'default'
        }

    def __get_query(self, comment):
        return {
            'metadata': {
                'user': self._user,
                'owner': self._owner,
                'token': self.__token,
                'message': comment
            },
            'commands': self.__commands
        }

    def get_topic_list(self, cluster, consumer=None):
        url = '%s:8999/pull/list' % self.get_server(cluster)
        if consumer:
            url += '?client=%s' % consumer
        topics = self.__try_request(url)

        if not topics:
            topics = ''
        return topics.split()

    def create_topic(self, origin_cluster, clusters, ident, logtype, partitions, consumer):
        topic = self.get_topic_name(ident, logtype)
        for dc in clusters:
            topic_key = self.__get_topic_key(origin_cluster, dc, topic)
            self.__commands.append(self.__get_command_create_topic(topic_key, partitions))
            self.__commands.append(self.__get_command_change_acl('create', dc, consumer, topic_key))

    def delete_topic(self, origin_cluster, clusters, ident, logtype, consumer=None):
        topic = self.get_topic_name(ident, logtype)
        for dc in clusters:
            topic_key = self.__get_topic_key(origin_cluster, dc, topic)
            if consumer:
                self.__commands.append(self.__get_command_change_acl('delete', dc, consumer, topic_key))
            self.__commands.append(self.__get_command_delete_topic(topic_key))

    def run_query(self, comment):
        query = self.__get_query(comment)
        logging.debug(query)
        result = self.__try_request(self.__command_url, query) is not None
        self.__commands = []
        logging.info('query result={result}: {comment}'.format(result=result, comment=comment))
        return result


class TestManager(Api):
    def __init__(
        self,
        owner='saas-refresh',
        user='saas-robot',
        token='',
        consumer='saas-tester',
        topic_count=1,
        origin_cluster='man',
        clusters=['man'],
        partitions=1
    ):
        Api.__init__(self, owner, user, token)
        self._consumer = consumer
        self._topic_count = topic_count
        self._origin_cluster = origin_cluster
        self._clusters = clusters
        self._partitions = partitions
        self._ident = 'saas-testing-{uid}-{ts}-{task_id}'.format(
            uid=uuid.uuid4().hex,
            ts=int(time.time()),
            task_id=get_sandbox_task_id()
        )

    def __enter__(self):
        logging.info('Create {count} topics with ident={ident}'.format(count=self._topic_count, ident=self._ident))
        self._create_topics()

    def __exit__(self, type, value, traceback):
        logging.info('Deleting topics')
        self._delete_topics()

    def _get_logtype(self, number):
        return 'shard-{number}'.format(number=number)

    def _check_topics_in_cluster(self, cluster, consumer=None):
        topics = self.get_topic_list(cluster, consumer)
        for number in range(self._topic_count):
            topic = self.get_topic_name(self._ident, self._get_logtype(number), cluster)
            found = False
            for t in topics:
                if t.endswith(topic):
                    found = True
                    break
            if not found:
                logging.debug('Topic {topic} not found in {cluster}'.format(topic=topic, cluster=cluster))
                return False
        return True

    def _wait_topics(self, start_wait_time=10, retries=5):
        wait_time = start_wait_time
        for retrie in range(retries):
            time.sleep(wait_time)
            topics_exsists = True
            for cluster in self._clusters:
                if not self._check_topics_in_cluster(cluster, self._consumer):
                    topics_exsists = False
                    break
            if topics_exsists:
                return True
            wait_time *= 2
        return False

    def __create_topics(self):
        for number in range(self._topic_count):
            self.create_topic(
                self._origin_cluster,
                self._clusters,
                self._ident,
                self._get_logtype(number),
                self._partitions,
                self._consumer
            )
        comment = 'SaaS tests: create {count} topics with ident={ident}'.format(count=self._topic_count, ident=self._ident)
        self.run_query(comment)
        if not self._wait_topics():
            logging.error('Topics not found')
            return False
        logging.info('Topics sucsessfully created')
        return True

    def _create_topics(self):
        os.environ['PQ_TESTS_SERVER'] = ''
        os.environ['PQ_TESTS_IDENT'] = ''
        os.environ['PQ_TESTS_TOPIC_COUNT'] = ''
        if not self.__create_topics():
            raise Exception('Unable to create topics for test')
        os.environ['PQ_TESTS_SERVER'] = self.get_server()
        os.environ['PQ_TESTS_IDENT'] = self._ident
        os.environ['PQ_TESTS_TOPIC_COUNT'] = str(self._topic_count)

    def _delete_topics(self):
        for number in range(self._topic_count):
            self.delete_topic(
                self._origin_cluster,
                self._clusters,
                self._ident,
                self._get_logtype(number),
                self._consumer
            )
        self.run_query('SaaS tests: delete {count} topics with ident={ident}'.format(count=self._topic_count, ident=self._ident))
