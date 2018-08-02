# University of Minnesota NLP-ADAPT (Artifact Discovery and Preparation Toolkit) 

The Artifact Discovery and Preparation Toolkit Kubernetes Cluster (NLP-ADAPT-Kube) is designed to allow researchers to increase the scale of the tools found in the [NLP-ADAPT VM](https://github.com/nlpie/nlp-adapt) for processing larger volumes of data for inter-system comparisons and ensembling of multiple annotator engines. 

### Project Goals
Scalability and performance: Ultimately, NLP-ADAPT-Kube will allow researchers to distribute annotator engine processing in parallel across multiple systems. 

Usability: We simplify the process of deploying a kubernetes cluster: We try to minimize system administrative tasks by utilizing minimal utilities, including

- `ansible-playbook` for building local dependencies
- `docker` for building Docker images
- `kubctl` for deployment of kubernetes cluster
- `argo` for workflow management of kubernetes cluster
- `minikube` for localized testing of kubernetes cluster

### Status of Project

NLP-ADAPT-Kube is currently in proof of concept and testing. We have utilized MiniKube VM for to simulating a Kubernetes cluster to develop and test serial processing capabilities within our workflow on a single device.

We will move from use of single machine using MiniKube VM  to native Linux OS access using virtualized Kubernetes overlay network to manage internal requests within the workflow. The other significant change is that nlp-annotation will occur in parallel across nodes.







