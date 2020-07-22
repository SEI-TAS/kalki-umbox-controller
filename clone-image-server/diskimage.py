# 
#  Kalki - A Software-Defined IoT Security Platform
#  Copyright 2020 Carnegie Mellon University.
#  NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS" BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.
#  Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.
#  [DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see Copyright notice for non-US Government use and distribution.
#  This Software includes and/or makes use of the following Third-Party Software subject to its own license:
#  1. Google Guava (https://github.com/google/guava) Copyright 2007 The Guava Authors.
#  2. JSON.simple (https://code.google.com/archive/p/json-simple/) Copyright 2006-2009 Yidong Fang, Chris Nokleberg.
#  3. JUnit (https://junit.org/junit5/docs/5.0.1/api/overview-summary.html) Copyright 2020 The JUnit Team.
#  4. Play Framework (https://www.playframework.com/) Copyright 2020 Lightbend Inc..
#  5. PostgreSQL (https://opensource.org/licenses/postgresql) Copyright 1996-2020 The PostgreSQL Global Development Group.
#  6. Jackson (https://github.com/FasterXML/jackson-core) Copyright 2013 FasterXML.
#  7. JSON (https://www.json.org/license.html) Copyright 2002 JSON.org.
#  8. Apache Commons (https://commons.apache.org/) Copyright 2004 The Apache Software Foundation.
#  9. RuleBook (https://github.com/deliveredtechnologies/rulebook/blob/develop/LICENSE.txt) Copyright 2020 Delivered Technologies.
#  10. SLF4J (http://www.slf4j.org/license.html) Copyright 2004-2017 QOS.ch.
#  11. Eclipse Jetty (https://www.eclipse.org/jetty/licenses.html) Copyright 1995-2020 Mort Bay Consulting Pty Ltd and others..
#  12. Mockito (https://github.com/mockito/mockito/wiki/License) Copyright 2007 Mockito contributors.
#  13. SubEtha SMTP (https://github.com/voodoodyne/subethasmtp) Copyright 2006-2007 SubEthaMail.org.
#  14. JSch - Java Secure Channel (http://www.jcraft.com/jsch/) Copyright 2002-2015 Atsuhiko Yamanaka, JCraft,Inc. .
#  15. ouimeaux (https://github.com/iancmcc/ouimeaux) Copyright 2014 Ian McCracken.
#  16. Flask (https://github.com/pallets/flask) Copyright 2010 Pallets.
#  17. Flask-RESTful (https://github.com/flask-restful/flask-restful) Copyright 2013 Twilio, Inc..
#  18. libvirt-python (https://github.com/libvirt/libvirt-python) Copyright 2016 RedHat, Fedora project.
#  19. Requests: HTTP for Humans (https://github.com/psf/requests) Copyright 2019 Kenneth Reitz.
#  20. netifaces (https://github.com/al45tair/netifaces) Copyright 2007-2018 Alastair Houghton.
#  21. ipaddress (https://github.com/phihag/ipaddress) Copyright 2001-2014 Python Software Foundation.
#  DM20-0543
#
#
#!/usr/bin/env python

import os.path
import subprocess
import sys


# Simple structure to represent a disk image based on a qcow2 file format.
class DiskImage(object):

    def __init__(self, disk_image_filepath):
        self.filepath = os.path.abspath(disk_image_filepath)

    def create_linked_qcow2_image(self, destination_disk_image_filepath):
        """ Creates a new qcow2 image referencing the current image. """

        # Check if the source disk image file exists.
        if not os.path.exists(self.filepath):
            raise Exception("Source image file does not exist (%s)." % self.filepath)

        # Check if the new disk image file already exists.
        if os.path.exists(destination_disk_image_filepath):
            # This is an error, as we don't want to overwrite an existing disk image with a source.
            raise Exception("Destination image file already exists (%s). Will not overwrite existing image." % destination_disk_image_filepath)

        # We need to use the qemu-img command line tool for this.
        # Note that we set the source file as its backing file. This is stored in the qcow2 file.
        # Note that we also use 4K as the cluster size, since it seems to be the best compromise.
        print "Creating qcow2 image %s based on source image %s..." % (destination_disk_image_filepath, self.filepath)
        sys.stdout.flush()
        image_tool_command = 'qemu-img create -f qcow2 -o backing_file=%s,cluster_size=4096 %s' \
                           % (self.filepath, destination_disk_image_filepath)
        self.__run_image_creation_tool(image_tool_command)
        print 'New disk image created.'

        cloned_disk_image = DiskImage(destination_disk_image_filepath)
        return cloned_disk_image

    def __run_image_creation_tool(self, image_tool_command):
        """ Starts the image creation tool in a separate process, and waits for it."""
        print("Executing command: " + image_tool_command)
        sys.stdout.flush()
        tool_pipe = subprocess.PIPE
        tool_process = subprocess.Popen(image_tool_command, shell=True, stdin=tool_pipe, stdout=tool_pipe, stderr=tool_pipe)
        normal_output, error_output = tool_process.communicate()

        # Show errors, if any.
        if len(error_output) > 0:
            print error_output

        # Show output, if any.
        if len(normal_output) > 0:
            print normal_output
