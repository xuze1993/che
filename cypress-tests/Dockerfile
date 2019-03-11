FROM cypress/browsers:chrome69
ENV RUN_DIR=/root/cypress
RUN mkdir /root/cypress
COPY . /root/cypress/
RUN  cd /root/cypress/ && npm install
RUN /root/cypress/node_modules/.bin/cypress install
WORKDIR /root/cypress
