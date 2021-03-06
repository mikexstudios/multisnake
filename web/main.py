#!/usr/bin/env python
#
# Copyright 2007 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import wsgiref.handlers
from google.appengine.ext import webapp

# Imports for our stuff:
from controllers import MainController
from controllers import ShareController  
from controllers import ServerManagerController


def main():
    application = webapp.WSGIApplication([('/', MainController.Index), 
        ('/index', MainController.Index), 
        ('/play', MainController.Index),
        ('/share', ShareController.Index),
        ('/manager/getserver', ServerManagerController.GetServer),
        ('/manager/postserver', ServerManagerController.PostServer),
    ], debug=True)

    wsgiref.handlers.CGIHandler().run(application)

if __name__ == '__main__':
  main()
