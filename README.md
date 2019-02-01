# University of Minnesota NLP-ADAPT (Artifact Discovery and Preparation Toolkit) 

The Artifact Discovery and Preparation Toolkit Kubernetes Cluster (NLP-ADAPT-Kube) is designed to allow researchers to increase the scale of the tools found in the [NLP-ADAPT VM](https://github.com/nlpie/nlp-adapt) for processing larger volumes of data at for ensembling of multiple annotator engines. 

### Project Goals
Scalability and performance: NLP-ADAPT-Kube will allow researchers to distribute annotator engine processing in parallel across multiple systems. 

Usability: We simplify the process of deploying a kubernetes cluster by utilizing standard utilities, including

- `ansible-playbook` for building local dependencies
- `docker` for building Docker images
- `kubectl` for deployment and monitoring of kubernetes cluster
- `argo` for workflow management of kubernetes cluster
- `kubeadm` for simplification of creating a multi-node kubernetes cluster
- `minikube` for localized testing of kubernetes cluster

### Status of Project

We utilize a [`minikube`](https://kubernetes.io/docs/setup/minikube/) for testing as a singlge node Kubernetes cluster. We also successfully deployed a Kubernetes cluster across multiple nodes with a single master node using [`kubeadm`](https://kubernetes.io/docs/setup/independent/create-cluster-kubeadm/).

All local devlopment can be done using `minikube` and then pushed to a production cluster. Our production cluster uses barebones native Linux OS access using `calico` as a virtualized overlay network to manage internal requests within cluster.

If you are interested in usinging this, please submit an issue. We can provide assistance in installation and configuration, as well as providing requisite `Docker` images needed to run this workflow.

This project, including the [Wiki](https://github.com/nlpie/nlp-adapt-kube/wiki) is a work in progress...








