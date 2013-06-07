from lxml.html import parse, tostring
import sys
import re

def main(entity):
    table = {}
    doc = parse('http://en.wikipedia.org/wiki/' + entity).getroot()
    for infobox in doc.xpath('//table[contains(@class, "infobox")]'):
      infobox.getparent().remove(infobox)

    for references in doc.xpath('//ol[contains(@class, "references")]'):
      references.getparent().remove(references)


    sent = ""
    for element in doc.itertext():
      text = element.encode('ascii', 'ignore')
      if not text or len(text) > 400:
        continue
      split = text.split('.')
      if len(split) == 1:
        sent += split[0]
      else:
        sent += split[0]
        if (any(c.isalpha() for c in sent)):
          print sent + "."
        for s in split[1:-1]:
          if (any(c.isalpha() for c in s)):
            print s + "."
        sent = split[-1]
    if (any(c.isalpha() for c in sent)):
      print sent

    return

if __name__ == '__main__':
  main(sys.argv[1])
