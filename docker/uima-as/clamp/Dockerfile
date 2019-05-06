FROM openjdk:8-jre

ADD ClampUimaAs_1.6.0.pre /usr/share/clamp
COPY required/clamp-as  /usr/share/clamp
#COPY required/license/CLAMP.LICENSE /usr/share/clamp

CMD ["/usr/share/clamp/bin/deployClampPipeline.sh"] 
