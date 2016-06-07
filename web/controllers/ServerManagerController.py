'''
Server Manager for multisnake on google apps.
'''

import sys
import datetime
from operator import itemgetter #For sorting, >= python 2.4

from google.appengine.ext import webapp
from google.appengine.ext.webapp import template
from google.appengine.ext import db

from models import ServerStatsModel

#Select best servers by taking the distance of each from the "optimal average" number of clients.
OPTIMAL_CLIENTS_PER_SERVER = 30;
SERVERS_TO_REPORT = 3;

class GetServer(webapp.RequestHandler):
    def get(self):
        #Delete stale servers
        ServerStatsModel.clear_stale_servers()
    
        query = db.Query(ServerStatsModel.Server)
        if (query.count() <= 0):
            self.response.out.write('NULL')
        else:
            #We have servers, now lets process the data and create an output.
            results = query.fetch(limit=100) #fetch requires a limit
    
            server_dict = {}
            for result in results:
                #Put everything in a dictionary
                server_dict[result.hostname] = result.num_clients
    
                #Testing out datetime stuff: (There is a bug in windows implementation.)
                #self.response.out.write(str(result.timestamp.time().hour)+'<br />')
                #self.response.out.write(result.hostname+'|'+str(result.num_clients)+'|'+result.timestamp.ctime()+'<br />')
                #self.response.out.write(datetime.datetime.utcnow().ctime())
                #self.response.out.write(result.hostname)
    
            for server, num_clients in server_dict.iteritems():
                #self.response.out.write(server+' '+str(num_clients))
                server_dict[server] = abs(num_clients - OPTIMAL_CLIENTS_PER_SERVER)
    
            #Now sort the dictionary by "num_clients" which is actually the distance from optimial
            server_dict_array = sorted(server_dict.items(), key=itemgetter(1)) #sort by value
    
            #Extract only the hostnames in the sorted order. Somehow the sorting above morphed
            #this into an array of tuples. Sucks ass.
            server_hostnames = list()
            for each_server_entry in server_dict_array:
                #print each_server_entry[0]+str(each_server_entry[1])
                server_hostnames.append(each_server_entry[0])
    
            #Finally, print out server output!
            self.response.out.write(','.join(server_hostnames))


class PostServer(webapp.RequestHandler):
    def get(self):
        key = self.request.get('key')

        #Check key
        if(key != 'Q7t1582VHWoeVE2ITMgrS'):
            self.response.out.write('Newb')
            return

        try:
            host = self.request.get('host')
            num_clients = int(self.request.get('num_clients'))
        except ValueError:
            self.response.out.write('Invalid inputs!')

        #Delete stale servers
        ServerStatsModel.clear_stale_servers()

        #Now write/update the database
        query = db.Query(ServerStatsModel.Server)
        query.filter('hostname =', host) #trim doesn't seem to work on host

        #TODO: Include error messages for put()'s?
        if(query.count() <= 0):
            #Server doesn't exist, create it. For some dumb-ass reason, when fields are required,
            #you must specify them during instantiation. You can't specify them later.
            server = ServerStatsModel.Server(hostname = host, num_clients = num_clients)
            #server.hostname = host
            #server.num_clients = num_clients
            server.put()
            self.response.out.write('New server added!')

        else:
            #Server exists, update it.
            result = query.fetch(limit=1)
            result[0].num_clients = num_clients
            result[0].put()
            self.response.out.write('Server updated!')
