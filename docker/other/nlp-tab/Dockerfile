FROM node:7.8

ADD nlptab-webapp /usr/share/nlptab 

RUN git clone https://github.com/nlpie/nlptab-webapp.git /usr/share/nlptab

RUN mkdir -p /data/data_in

WORKDIR /usr/share/nlptab 

RUN npm install
RUN npm install -g bower gulp node-sass
RUN npm install gulp

# needed to run bower as root
RUN echo '{ "allow_root": true }' > /root/.bowerrc
RUN bower install 

RUN gulp

COPY required/* ./

EXPOSE 8000
 
CMD ["gulp", "webserver"]

