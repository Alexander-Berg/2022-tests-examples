echo "Индексируем документы по sitemap.xml"

echo "получаем урлы из sitemap.xml"
PYTHONPATH=python-sitemap python get_urls_by_sitemap.py -f sitemap_example.xml

echo "скачиваем документы через zoracl"
rm -rf /tmp/index_site/data && mkdir -p /tmp/index_site/data
zoracl fetch --source any --server zora.yandex.net -i urls_sitemap_example.xml.q --format=document --output +/tmp/index_site/data/data --of-limit-records=1

echo "отправляем на индексацию"
python index_site.py -c index_site.conf
