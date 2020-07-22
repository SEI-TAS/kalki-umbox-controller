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
import os
import sys

from flask import Flask
from flask_restful import Api, Resource, reqparse

import diskimage

# Path to stored VM umbox images in data node.
DATA_NODE_IMAGES_PATH = "./images/"
INSTANCES_FOLDER = "instances"

BASE_URL = "/clone"
INSTANCE_PATH_KEY = "instance_path"
STATUS_KEY = "status"
OK_VALUE = "ok"


def get_instance_folder():
    return os.path.join(os.path.abspath(DATA_NODE_IMAGES_PATH), INSTANCES_FOLDER)


def get_instance_path(instance_name):
    return os.path.join(get_instance_folder(), instance_name + ".qcow2")


class ImageClone(Resource):
    """Resource for handling cloning of images."""

    def post(self, image_file_name, instance_name):
        """Clones an existing disk image for a new instance."""
        # Create a new disk image object based on the given image filename.
        image_path = os.path.join(os.path.abspath(DATA_NODE_IMAGES_PATH), image_file_name)
        template_image = diskimage.DiskImage(image_path)

        # Check if folder doesn't exist, and create it if so.
        if not os.path.exists(get_instance_folder()):
            os.makedirs(get_instance_folder())

        # Clone the image for a new instance image.
        instance_disk_path = get_instance_path(instance_name)
        print("Cloning image file " + image_path + " as " + instance_disk_path)
        sys.stdout.flush()
        template_image.create_linked_qcow2_image(instance_disk_path)

        return {INSTANCE_PATH_KEY: instance_disk_path}

    def delete(self, image_file_name, instance_name):
        """Remove an exising instance image."""
        # Clone the image for a new instance image.
        instance_disk_path = get_instance_path(instance_name)
        print("Removing disk image in following path: " + instance_disk_path)
        os.remove(instance_disk_path)

        return {STATUS_KEY: OK_VALUE}


def main():
    print("Images path: " + os.path.abspath(DATA_NODE_IMAGES_PATH))

    app = Flask(__name__)

    api = Api(app)
    api.add_resource(ImageClone, BASE_URL + "/<string:image_file_name>/<string:instance_name>")

    app.run(host="0.0.0.0", debug=True)


if __name__ == "__main__":
    main()
