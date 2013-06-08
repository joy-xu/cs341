from lxml.html import parse, tostring
import sys
import re

def main(entity):
    table = {}
    doc = parse('http://en.wikipedia.org/wiki/' + entity).getroot()
    infobox = doc.xpath('//table[contains(@class, "infobox")]')
    if len(infobox) > 1:
      print "More than one infobox! I'm confused!"
      return
    for element in infobox[0].xpath('//br'):
      if element.text is None:
        element.text = '\n'
      else:
        element.text = element.text + '\n'

    for row in infobox[0].xpath('child::tr'):
      try:
        key = row.xpath('child::th')[0].text_content().encode('ascii','ignore')
        val = row.xpath('child::td')[0].text_content()
        '''
        val = []
        for element in row.xpath('child::td')[0].iter():
          print("%s - %s" % (element.tag, element.text))
          if element.text is None:
            continue
          else:
            val.extend(element.text.split(','))
        '''
      except:
        continue
      
      temp = table.get(key, [])
      temp.extend([i.strip().encode('ascii','ignore') for i in re.split('\n|,', val) if i])
      table[key] = temp

    print table

if __name__ == '__main__':
  main(sys.argv[1])
