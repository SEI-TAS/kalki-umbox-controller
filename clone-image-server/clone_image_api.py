import os

from flask import Flask
from flask_restful import Api, Resource, reqparse

import diskimage

# Path to stored VM umbox images in data node.
DATA_NODE_IMAGES_PATH = "/home/kalki/images/"
INSTANCES_FOLDER = "instances"

BASE_URL = "/clone"
INSTANCE_PATH_KEY = "instance_path"
STATUS_KEY = "status"
OK_VALUE = "ok"


def get_instance_path(instance_name):
    return os.path.join(DATA_NODE_IMAGES_PATH, INSTANCES_FOLDER, instance_name + ".qcow2")


class ImageClone(Resource):
    """Resource for handling cloning of images."""

    def post(self, image_file_name, instance_name):
        """Clones an existing disk image for a new instance."""
        # Create a new disk image object based on the given image filename.
        image_path = os.path.join(DATA_NODE_IMAGES_PATH, image_file_name)
        template_image = diskimage.DiskImage(image_path)

        # Clone the image for a new instance image.
        instance_disk_path = get_instance_path(instance_name)
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
    app = Flask(__name__)

    api = Api(app)
    api.add_resource(ImageClone, BASE_URL + "/<string:image_file_name>/<string:instance_name>")

    app.run(host="0.0.0.0", debug=True)


if __name__ == "__main__":
    main()
