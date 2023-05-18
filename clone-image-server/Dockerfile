FROM python:3.11-bullseye

# Trusted host configs used to avoid issues when running behind SSL proxies.
RUN pip config set global.trusted-host "pypi.org pypi.python.org files.pythonhosted.org"

# Install qemu-img
RUN apt-get update
RUN apt-get install -yqq qemu-utils

# CloneImage server is listening here.
EXPOSE 5000

# Copy python deps and install them
COPY requirements.txt /app/
WORKDIR /app
RUN pip install -r requirements.txt --default-timeout=100

# Copy the actual code
COPY clone_image_api.py /app/
COPY diskimage.py /app/

# Calling directly.
ENTRYPOINT ["python", "clone_image_api.py"]
