# minikube stop/delete
minikube --memory 3000 --cpus 2 start


# get docker images into minikube environ

docker save horcle/elastic | pv | (eval $(minikube docker-env) && docker load)
docker save horcle/biomedicus | pv | (eval $(minikube docker-env) && docker load)
docker save horcle/nlptab | pv | (eval $(minikube docker-env) && docker load)


#cd ~/development/nlp/nlpie/nlp-adapt/kube/biomedicus/
#docker build -t horcle/biomedicus .
#cd ../
#cd elastic_search/
#docker build -t horcle/elastic .
#cd ../
#cd nlp-tab/
#docker build -t horcle/nlptab .

# set minikube environ
eval $(minikube docker-env)

# TODO: test as replacement to manual edit of yml with inserted chucnk below
kubectl apply -f ~/development/nlp/nlpie/nlp-adapt/kube/kube-minio-configmap.yml
# NB: need to issue command right after starting minikube

# kubectl edit configmap workflow-controller-configmap -n kube-system
# add following snippet:
# 
#executorImage: argoproj/argoexec:v2.1.1
#     artifactRepository:
#       s3:
#         bucket: my-bucket
#         keyPrefix: prefix/in/bucket
#         endpoint: my-minio-endpoint.default:9000
#         insecure: true
#         accessKeySecret:
#           name: my-minio-cred
#           key: accesskey
#         secretKeySecret:
#           name: my-minio-cred
#           key: secretkey

# elevate permissions
kubectl create rolebinding default-admin --clusterrole=admin --serviceaccount=default:default

# install workflow controllers, etc.
argo install

# install artifact storage controller and stuffs
helm init
helm install stable/minio --name argo-artifacts --set service.type=LoadBalancer

# get minio utl
minikube service --url argo-artifacts-minio
# use credentials:
# AccessKey: AKIAIOSFODNN7EXAMPLE
# SecretKey: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
#
# add my-bucket

# start argo UI
kubectl patch svc argo-ui -n kube-system -p '{"spec": {"type": "LoadBalancer"}}'
minikube service -n kube-system --url argo-ui

# submit workflow
argo submit ~/development/nlp/nlpie/nlp-adapt/argo/biomedicus-elastic-nlptab-wf.yml
