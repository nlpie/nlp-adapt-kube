/**
 * The UIMA AS Broker url that is coordinating requests.
 */
brokerURL = "tcp://amq.default:61616"

/**
 * The endpoint name of the UIMA AS service to use for processing. The service will be
 * registered with the broker as this service name. Clients use the broker/service combination
 * to connect to this service.
 */
endpoint = "nlpadapt.clamp.outbound"

/**
 * Tell the service to persist the descriptors that are generated, deletes them by default.
 */
//deleteOnExit = false
//descriptorDirectory = "config/desc"

/**
 * Additional service configurations.
 */
casPoolSize = 200
CCTimeout = 1000
jamQueryIntervalInSeconds = 3600
jamResetStatisticsAfterQuery = false
jamServerBaseUrl  = "http://localhost:8080/jam"

/**
 * Example service parameters.
 */
isAsync=false
instanceNumber = 32
createTypes = false
