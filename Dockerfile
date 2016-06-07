# Pull base image.
FROM ubuntu:14.04

RUN apt-get update && \
    apt-get install -y ca-certificates vim-tiny screen wget unzip

# Set up our app files
RUN mkdir -p /usr/src/app
COPY . /usr/src/app

# For flash client
# This will generate `snake.swf`, which will be copied to web/views/releases/
RUN apt-get install -y make swfmill mtasc
WORKDIR /usr/src/app/client
RUN make && make release

# Set up game server
# This will generate `SnakeServer.jar` from version 0.6.5. The trunk code is
# buggy and uses significant CPU.
# Install Oracle's Java (http://askubuntu.com/a/637514)
#RUN apt-get install -y software-properties-common && \
#    add-apt-repository -y ppa:webupd8team/java && \
#    apt-get update && \
#    echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | sudo debconf-set-selections && \
#    apt-get install -y oracle-java7-set-default
RUN apt-get install -y ant openjdk-7-jdk
WORKDIR /usr/src/app/server/tags/0.6.5
RUN ant

# Set up appengine webserver
RUN apt-get install -y python #2.7.6 
WORKDIR /usr/src
RUN wget https://storage.googleapis.com/appengine-sdks/deprecated/140/google_appengine_1.4.0.zip && \
    unzip google_appengine_1.4.0.zip
# Prevent dev_appserver.py prompting for update
RUN echo "opt_in: false\ntimestamp: 1465240212.212144\n" > ~/.appcfg_nag
 
# Now run everything
# NOTE: port 843 is for the flash policy server, see SnakeConnection.as:25
#       port 10123 is for the java game server
EXPOSE 80
EXPOSE 843 
EXPOSE 10123 
VOLUME ["/usr/src/app"]
WORKDIR /usr/src/app
CMD ../google_appengine/dev_appserver.py -a 0.0.0.0 -p 80 web/ & \
    cd server/tags/0.6.5 && \
    java -Djava.net.preferIPv4Stack=true -server -jar dist/SnakeServer.jar -a 8
