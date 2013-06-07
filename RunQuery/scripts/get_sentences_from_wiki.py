from lxml.html import parse, tostring
import sys
import re

def clean_dom(doc):
    for infobox in doc.xpath('//table[contains(@class, "infobox")]'):
      infobox.getparent().remove(infobox)

    for references in doc.xpath('//ol[contains(@class, "references")]'):
      references.getparent().remove(references)

    for references in doc.xpath('//script'):
      references.getparent().remove(references)

    for references in doc.xpath('//style'):
      references.getparent().remove(references)

    for references in doc.xpath('//*[contains(@class, "footer")]'):
      references.getparent().remove(references)
    
    for references in doc.xpath('//*[contains(@class, "metadata")]'):
      references.getparent().remove(references)

    for references in doc.xpath('//*[contains(@class, "navbox")]'):
      references.getparent().remove(references)
    
    for references in doc.xpath('//*[contains(@class, "catlinks")]'):
      references.getparent().remove(references)
    
    for references in doc.xpath('//*[contains(@class, "persondata")]'):
      references.getparent().remove(references)
    
    for references in doc.xpath('//*[contains(@class, "portal")]'):
      references.getparent().remove(references)
    
    for references in doc.xpath('//*[contains(@id, "footer")]'):
      references.getparent().remove(references)
    
    for references in doc.xpath('//*[contains(@id, "navigation")]'):
      references.getparent().remove(references)
    
    for references in doc.xpath('//comment()'):
      references.getparent().remove(references)
    
def main(entity):
    table = {}
    doc = parse('http://en.wikipedia.org/wiki/' + entity).getroot()
    #print tostring(doc, pretty_print=True)
    #return
    clean_dom(doc)

    sent = ""
    for element in doc.itertext():
      text = element.encode('ascii', 'ignore')
      if not text or len(text) > 400:
        continue
      split = re.split('\.|\n', text)
      if len(split) == 1:
        sent += split[0]
      else:
        sent += split[0]
        if (any(c.isalpha() for c in sent)):
          print re.sub('\[[0-9]+\]', '', sent) + "."
        for s in split[1:-1]:
          if (any(c.isalpha() for c in s)):
            print re.sub('\[[0-9]+\]', '', s) + "."
        sent = split[-1]
    if (any(c.isalpha() for c in sent)):
      print re.sub('\[[0-9]+\]', '', sent) + "."

    return

if __name__ == '__main__':
  main(sys.argv[1])
