angular.module('nlptabApp')
  .constant('nlptabConfig', {
    instanceName: 'default',
    esServer: 'http://192.168.99.100:31345', // kube cluster IP address
    isSecure: true,
    isBio: false
  });
