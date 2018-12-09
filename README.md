# University of Minnesota NLP-ADAPT (Artifact Discovery and Preparation Toolkit) 

The Artifact Discovery and Preparation Toolkit Kubernetes Cluster (NLP-ADAPT-Kube) is designed to allow researchers to increase the scale of the tools found in the [NLP-ADAPT VM](https://github.com/nlpie/nlp-adapt) for processing larger volumes of data for inter-system comparisons and ensembling of multiple annotator engines. 

### Project Goals
Scalability and performance: NLP-ADAPT-Kube will allow researchers to distribute annotator engine processing in parallel across multiple systems. 

Usability: We simplify the process of deploying a kubernetes cluster: We try to minimize system administrative tasks by utilizing minimal utilities, including

- `ansible-playbook` for building local dependencies
- `docker` for building Docker images
- `kubctl` for deployment of kubernetes cluster
- `argo` for workflow management of kubernetes cluster
- `kubeadm` for simplification of creating a multi-node kubernetes cluster
- `minikube` for localized testing of kubernetes cluster

### Status of Project


NLP-ADAPT-Kube is currently in optimization and testing. We have utilized MiniKube VM for to simulating a Kubernetes cluster to develop and test serial processing capabilities within our workflow on a single device. We have also successfully deployed a Kubernetes cluster across multiple nodes with a single master using `kubeadm.`

NLP-ADAPT-Kube has undergone proof of concept utilizing MiniKube VM for to simulate a single node Kubernetes cluster in order to develop and test processing capabilities within our workflow on a single device. 

We are currently testing native Linux OS access using a virtualized Kubernetes overlay network (`Calico`) to manage internal requests within the workflow across multiple nodes, with the `Kubernetes` scheduler handling parallel nlp-annotation across nodes.

If you are interested in testing this, please submit an issue. We can provide assistance in installation and configuration, as well as providing requisite `Docker` images needed to run this workflow.

This project, including the [Wiki](https://github.com/nlpie/nlp-adapt-kube/wiki) is a work in progress...








