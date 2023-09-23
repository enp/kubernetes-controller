#!/usr/bin/env groovy

@groovy.util.logging.Slf4j
class JobController {
    private kubernetes
    private namespace = 'default'
    private results = []
    private informer
    def start() {
        log.info('START')
        kubernetes = new io.fabric8.kubernetes.client.KubernetesClientBuilder().build()
        informer = kubernetes.batch().v1().jobs().inNamespace(namespace).inform(
            new io.fabric8.kubernetes.client.informers.ResourceEventHandler<io.fabric8.kubernetes.api.model.batch.v1.Job>() {
                void onAdd(job) {}
                void onDelete(job, boolean deletedFinalStateUnknown) {}
                void onUpdate(oldJob, newJob) {
                    def jobType = newJob.status.conditions.type
                    if (jobType) {
                        def jobName = newJob.metadata.name
                        def jobLog = kubernetes.batch().v1().jobs().inNamespace(namespace).withName(jobName).log
                        results.add([name: jobName, type: jobType, log: jobLog])
                        log.info('JOB RESULTS : {}({}) : {}', jobName, jobType, jobLog)
                        kubernetes.resource(newJob).delete()
                    }
                }
            }
        )
    }
    def run(message) {
        def job = kubernetes.load(new FileInputStream('job.yaml')).items()[0]
        job.spec.template.spec.containers[0].command[1] = message
        kubernetes.batch().v1().jobs().inNamespace('default').create(job).metadata.name
    }
    def list() {
        groovy.json.JsonOutput.toJson(results)
    }
    def stop() {
        informer.close()
        kubernetes.close()
        log.info('STOP')
    }
}

@groovy.util.logging.Slf4j
@groovy.transform.TupleConstructor
class WebController {
    def jobController
    private httpPort
    private httpServer
    def start() {
        log.info('START')
        httpPort = System.getenv('HTTP_PORT') ?: '8080'
        httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(httpPort.toInteger()), 0)
        httpServer.with {
            createContext('/') { httpExchange ->
                def method = httpExchange.requestMethod
                def message = httpExchange.requestBody.text
                def response = ''
                log.info('request from {} : {} : {}', httpExchange.remoteAddress, method, message)
                httpExchange.responseHeaders.add('Content-Type', 'text/plain')
                try {
                    switch (method) {
                        case 'POST':
                            response = jobController.run(message)
                            break
                        case 'GET':
                            response = jobController.list()
                            break
                    }
                    httpExchange.sendResponseHeaders(200, response.length())
                    httpExchange.responseBody.withWriter { it << response }
                } catch(Exception e) {
                    httpExchange.sendResponseHeaders(500, response.length())
                    httpExchange.responseBody.withWriter { e.printStackTrace(new PrintWriter(it)) }
                }
            }
            start()
        }
    }
    def stop() {
        httpServer.stop(0)
        log.info('STOP')
    }
}

def jobController = new JobController()
def webController = new WebController(jobController)

addShutdownHook {
    webController.stop()
    jobController.stop()
}

jobController.start()
webController.start()
