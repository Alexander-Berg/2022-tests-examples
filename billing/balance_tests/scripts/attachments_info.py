# coding: utf-8

import argparse
import logging
import os
import re
import sys
from collections import defaultdict

import attr
from lxml import etree


# import btestlib.reporter as reporter


@attr.s
class FileInfo(object):
    filepath = attr.ib()
    size = attr.ib()
    title = attr.ib(default=None)


def attach_info_from_xml(filepath):
    root = etree.parse(filepath)
    attachments = root.xpath("//attachment")
    return {attach.get('source'): attach.get('title') for attach in attachments}


def get_dir_and_outfile():
    parser = argparse.ArgumentParser(description='Get folder to search')
    parser.add_argument('datadir')
    # parser.add_argument('outfile')
    args = parser.parse_args()
    # return os.path.abspath(args.datadir), os.path.abspath(args.outfile)
    return os.path.abspath(args.datadir)


def get_method_name(attachpath):
    with open(attachpath, 'rb') as f:
        firstline = f.readline()
        return firstline[0:firstline.find('(')]


if __name__ == '__main__':
    logging.basicConfig(stream=sys.stdout, level=logging.INFO, format='')

    # directory, outfile = get_dir_and_outfile()
    directory = get_dir_and_outfile()

    filepaths = [os.path.join(directory, filepath) for filepath in os.listdir(directory)]

    xmlfiles = [filepath for filepath in os.listdir(os.path.abspath(directory))
                if filepath.endswith('-testsuite.xml')]

    attaches = {}
    for xmlfile in xmlfiles:
        attaches.update(attach_info_from_xml(os.path.join(directory, xmlfile)))

    files = [FileInfo(filepath=filepath,
                      title=attaches.get(os.path.split(filepath)[-1], u'Файлы аллюра'),
                      size=os.path.getsize(filepath) / 1024.0)
             for filepath in filepaths]

    sizes = defaultdict(lambda: 0)
    count = defaultdict(lambda: 0)
    timings = defaultdict(lambda: [])
    total_size = 0.0
    for file_ in files:
        total_size += file_.size
        label = re.sub('^\(\d+\.\d+s\) (.*)', r'\1', file_.title)
        sizes[label] += file_.size
        count[label] += 1
        timing = re.findall('^\((\d+\.\d+)s\) .*', file_.title)
        if timing:
            timings[label].append(float(timing[0]))

    logging.info(u'Общий размер (КБ) {}\nКол-во файлов {}\n'.format(total_size, len(files)))

    logging.info(u'{:<15s} {:<10s} {:<10s} {:<10s}\n'.format(u'Размер (КБ)', u'Доля', u'Кол-во', u'Категория') + \
                 u'\n'.join(
                     [u'{:<15.3f} {:<10.3f} {:<10d} {}'.format(size, size / total_size, count[category], category)
                             for category, size in sorted(sizes.iteritems(), key=lambda x: -x[1])]))

    total_count = float(len(files))
    logging.info(u'\n\n{:<10s} {:<10s} {:<15s} {:<10s}\n'.format(u'Кол-во', u'Доля', u'Размер (КБ)', u'Категория') + \
                 u'\n'.join([u'{:<10d} {:<10.3f} {:<15.3f} {}'.format(cnt, cnt / total_count,
                                                                    sizes[category], category)
                             for category, cnt in sorted(count.iteritems(), key=lambda x: -x[1])]))

    logging.info(u'\n\nВремя выполнения методов в секундах\n')
    data = [(len(times), sum(times) / count[method], max(times), min(times), sum(times), method)
            for method, times in timings.iteritems()]
    logging.info(u'{:<10s} {:<10s} {:<15s} {:<15s} {:<15s} {}\n'.format(u'Вызовов', u'Среднее', u'Максимальное',
                                                                        u'Минимальное', u'Общее', u'Метод') + \
                 u'\n'.join([u'{:<10d} {:<10.3f} {:<15.3f} {:<15.3f} {:<15.3f} {}'.format(cnt, avgt, maxt, mint, sumt,
                                                                                          method)
                             for cnt, avgt, maxt, mint, sumt, method in sorted(data, key=lambda x: (-x[4], -x[0]))]))

    # with open(outfile, 'w+') as f:
    #     f.write(json.dumps(sorted([attr.asdict(file_) for file_ in files], key=lambda x: -x['size']),
    #                        encoding='utf-8'))
