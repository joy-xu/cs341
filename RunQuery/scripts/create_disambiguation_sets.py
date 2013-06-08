from lxml.html import parse, tostring
import sys
import re

def main(entity):
    print "creating disambiguation file for " + entity
    try:
      oldfile = open('../data/entities_expanded_new/' + entity, 'r')
    except:
      print "Error opening old file"
      return
    newfile = open('../data/entities_expanded_updated/' + entity, 'w')
    
    for line in oldfile:
      newfile.write(line)
    oldfile.close()
    
    try:
      doc = parse('http://en.wikipedia.org/wiki/' + entity).getroot()
    except:
      print "wiki url for " + entity + "doen't exist"
      return

    try: 
      scripts = doc.xpath('//script')
      for script in scripts:
        try:
          temp = re.split('"categories"', tostring(script))[1]
          categories = temp[temp.index('[')+1:temp.index(']')].split(',')
          categories = [i.strip().replace('"', '').encode('ascii', 'ignore') for i in categories if i]
        except:
          continue
    except:
      print "error extracting categories for " + entity
      

    for category in categories:
      if 'articles' not in category.lower():
        newfile.write(category + '\n')      
    newfile.close()

if __name__ == '__main__':
  main(sys.argv[1])
