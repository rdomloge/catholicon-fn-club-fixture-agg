sudo faas-cli build -f function.yml --image dockerfile-test && \
sudo docker tag function rdomloge/dockerfile-test && \
sudo docker push rdomloge/dockerfile-test
