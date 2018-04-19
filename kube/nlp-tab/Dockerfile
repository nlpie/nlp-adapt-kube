FROM node:7.8

# required for the node-sass binary from https://github.com/sass/node-sass/issues/1589
RUN npm install -g bower gulp node-sass
RUN npm rebuild node-sass

RUN apt-get update
RUN apt-get install -y vim

RUN mkdir -p /app

WORKDIR /app

ADD nlptab-webapp nlptab 

WORKDIR nlptab

# needed, since am grabbing from version built on OS X:
RUN gulp clean
RUN npm rebuild node-sass

# needed to run bower as root
RUN echo '{ "allow_root": true }' > /root/.bowerrc
RUN gulp

EXPOSE 8000
 
CMD ["gulp", "webserver"]

