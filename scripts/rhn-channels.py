#!/usr/bin/python

# Authors jsherril and jsefler
# Given a credentials and a hosted RHN server, this script is used to list the
# RHN Classic channels available.

import sys, time
from xmlrpclib import Server
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-u", "--username", dest="username", help="Username")
parser.add_option("-p", "--password", dest="password", help="Password")
parser.add_option("-b", "--basechannel", dest="basechannel", help="List child channels for only this base channel")
parser.add_option("-n", "--no-custom", dest="nocustom", action="store_true", default=False, help="Attempt to filter out custom channels (identified by no gpgkey)")
parser.add_option("-a", "--available", dest="available", action="store_true", default=False, help="Only list the child channels that currently have available entitlements")
parser.add_option("-s", "--server", dest="server", help="Server hostname rhn.redhat.com", default="rhn.redhat.com")
(options, args) = parser.parse_args()

if not options.username or not options.password:
   parser.print_usage()
   sys.exit(1)

# create an api connection to the server
# RHN API documentation: https://access.stage.redhat.com/knowledge/docs/Red_Hat_Network/
client = Server("https://%s/rpc/api/" % options.server)
#sessionKey = client.auth.login(options.username, options.password)
sessionKey = None
count = 0
while (sessionKey == None):
    if count > 10:
        print "Giving up trying to authenticate to RHN API..."
        sys.exit(-1)
    try:
        sessionKey = client.auth.login(options.username, options.password)
    except Exception, e:
        print "Unexpected error:", e
        count += 1
        time.sleep(10)

# find all the available parent/base channels and their child channels
parents = []
child_map = {}
chan_list = client.channel.listSoftwareChannels(sessionKey)
for chan in chan_list:
    if chan["channel_parent_label"] == "":
        parents.append(chan["channel_label"])
    else:
       if not child_map.has_key(chan["channel_parent_label"]):
           child_map[chan["channel_parent_label"]] = []
       child_map[chan["channel_parent_label"]].append(chan["channel_label"])


# print a tree view of the channels
for parent in parents:
    
    if options.basechannel and options.basechannel != parent:
        continue
    
    if options.nocustom:
        details = client.channel.software.getDetails(sessionKey, parent)
        if details["channel_gpg_key_url"] == "":
            continue
    print parent
    
    if child_map.has_key(parent):
        for child in child_map[parent]:        
            
            if options.available:
                availableEntitlements = client.channel.software.availableEntitlements(sessionKey, child)
                if availableEntitlements <= 0:
                    continue
            
            if options.nocustom:
                details = client.channel.software.getDetails(sessionKey, child)
                if details["channel_gpg_key_url"] == "":
                    continue
            
            print "  " + child
