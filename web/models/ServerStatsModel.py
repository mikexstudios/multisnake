import datetime

from google.appengine.ext import db

SERVER_UPDATE_TIMEOUT = 40 #seconds

class Server(db.Model):
	hostname = db.StringProperty(required=True)
	num_clients = db.IntegerProperty(required=True)
	#auto_now does the "last_modified" date update.
	timestamp = db.DateTimeProperty(auto_now=True)
	

def clear_stale_servers():
	#How to use dates and times with SQL:
	#http://groups.google.com/group/google-appengine/browse_thread/thread/6113d831d2a0de76/992dde174d901fe3?lnk=gst&q=gql+NOW#992dde174d901fe3
	timeout_ago = datetime.datetime.utcnow() - datetime.timedelta(seconds=SERVER_UPDATE_TIMEOUT)

	query = db.Query(Server)
	#query = db.all() #Operate on all rows
	query.filter('timestamp <', timeout_ago)
	
	results = query.fetch(100)
	for each_entry in results:
		#print each_entry.hostname
		each_entry.delete()
