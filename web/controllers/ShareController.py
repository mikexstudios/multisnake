from google.appengine.ext import webapp
from google.appengine.ext.webapp import template

class Index(webapp.RequestHandler):
    def get(self):
        self.response.out.write(template.render('views/share.html', None))
