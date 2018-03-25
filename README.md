<link href="https://fonts.googleapis.com/css?family=Righteous" rel="stylesheet">

<p align="center">
<h1 style="font-family: 'Righteous', cursive;">AGORA</h1>
</p>

Distributed file-systems have become both essential and ubiquitous in rendering modern Internet services at scale. Traditional and established distributed file-systems, such as Google's GFS and Apache's HDFS have been largely based-off the concept of master/slave clusters. That is, network nodes are organised into logical groups, in which typically one node is elected to direct operation, and other nodes serve merely as partial data stores. These file-systems have historically suffered from the performance and availability issues associated with fixed single-leader architectures.

With the advent of fault-tolerant distributed-ledger technologies, such as Hashgraph and Blockchain, it is now possible to build fully distributed, fully federated storage applications. A distributed file-system without a central authority has many desirable qualities. An empirical study is required, however, to establish the feasibility in terms of performance and scalability of such applications in comparison to current industry standards.

In this paper, I will evaluate an experimental file-system of my own creation, which I have named AGORA. First I will explain its implementation in relation to the Hashgraph consensus algorithm. I will then assess the merit of this application based on the model proposed by (reference), founded on concrete empirical data obtained through tests. I will then discuss some techniques that aid, hinder or otherwise augment the application's operation.