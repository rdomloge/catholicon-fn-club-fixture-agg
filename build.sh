sudo faas-cli build -f function.yml --image catholicon-fn-club-fixture-agg && \
sudo docker tag function rdomloge/catholicon-fn-club-fixture-agg && \
sudo docker push rdomloge/catholicon-fn-club-fixture-agg
