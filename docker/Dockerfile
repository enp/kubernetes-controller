FROM groovy:4.0-jdk17

RUN mkdir $HOME/.groovy/lib

RUN grape install org.slf4j slf4j-simple $(ls /opt/groovy/lib/ | grep slf4j-api | sed -e 's/slf4j-api-//' | sed -e 's/.jar//')
RUN find $HOME/.groovy/grapes/ -name *.jar -exec mv {} $HOME/.groovy/lib/ \;
RUN rm -rf $HOME/.groovy/grapes/*

RUN grape install io.fabric8 kubernetes-client '[6,)'
RUN grape install io.fabric8 kubernetes-client-api '[6,)'
RUN grape install io.fabric8 kubernetes-httpclient-jdk '[6,)'
RUN rm -rf $HOME/.groovy/grapes/org.slf4j
RUN rm -rf $HOME/.groovy/grapes/io.fabric8/*okhttp*
RUN rm -rf $HOME/.groovy/grapes/com.squareup.*
RUN find $HOME/.groovy/grapes/ -name *.jar -exec mv {} $HOME/.groovy/lib/ \;
RUN rm -rf $HOME/.groovy/grapes/*

USER root

COPY ./app /app

RUN sed -i '/add-modules/d' /opt/groovy/bin/startGroovy*

USER groovy

WORKDIR /app

CMD /app/app.groovy
