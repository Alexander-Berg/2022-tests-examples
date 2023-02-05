.PHONY: all install clean

LAYERNAME=test
	
validate:
	xmllint --xinclude map.xml | sed 's/xml:base="[^"]*"//g' > map.xml.validate; xmllint --noout --schema /usr/share/yandex/maps/renderer/schema/Renderer.xsd map.xml.validate;rm map.xml.validate

