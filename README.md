Kubernetes Example Controller Application
=========================================

Example application demonstrates how to run job in kubernetes on incoming HTTP request, cleanup and save results

We can use k0s as simplest kubernetes implementation to run this application, no other requirements except docker installed:

```
docker run -d --name k0s --hostname k0s --privileged -v /var/lib/k0s -p 6443:6443 -p 30080:30080 docker.io/k0sproject/k0s k0s controller --single
```

Wait a bit until all pods in kube-system namespace will be running:

```
docker exec -it k0s kubectl get pods --all-namespaces
```

And copy configuration from container:

```
docker exec k0s cat /var/lib/k0s/pki/admin.conf > $HOME/.kube/config
```

Now we can build and run example application:

```
docker build -t evnp/kubernetes-controller -f docker/Dockerfile .
docker run -it --rm --network=host -v $HOME/.kube:/home/groovy/.kube evnp/kubernetes-controller
```

As last step we can run some HTTP requests on another console to ensure if example application works:

```
curl -v -d 'HELLO' -H 'Content-Type: text/plain' localhost:8080
curl -v localhost:8080
```

Also we can push image to docker registry and deploy application to kubernetes:

```
docker push evnp/kubernetes-controller
kubectl apply -f kubernetes/app.yaml
```

Application is exposed as NodePort on port 30080, so we can use curl again
