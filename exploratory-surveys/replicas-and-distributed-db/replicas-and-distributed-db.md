# Replicas와 Distributed DB

- [Replicas와 Distributed DB](#replicas와-distributed-db)
  - [분산형 시스템의 Trade-off에 대하여](#분산형-시스템의-trade-off에-대하여)
  - [Read-only Replica](#read-only-replica)
  - [Sharding](#sharding)
  - [DB replication](#db-replication)
    - [Single-leader Replication 구조](#single-leader-replication-구조)
    - [Multi-leader Replication 구조](#multi-leader-replication-구조)
    - [Leaderless Replication 구조](#leaderless-replication-구조)
  - [본연구 주제](#본연구-주제)
  - [링크](#링크)

본 조사에서는 분산형 시스템에 대하여 배경 정리를 해 두겠습니다. Reference의 경우 링크로 해당 부분 옆에 바로 붙여 두겠습니다.

[Distributed DB](https://en.wikipedia.org/wiki/Distributed_database), 혹은 분산형 데이터베이스는 데이터가 물리적으로 다른 위치에 저장되어 있는 DB입니다. 같은 장소의 다른 컴퓨터, 혹은 네트워크를 통해 연결된 컴퓨터 등일 수 가 있습니다.

## 분산형 시스템의 Trade-off에 대하여

분산형 data store(DB뿐 아니라 파일시스템 등 data store 전잔 포함)의 디자인에서 [CAP Theorem](https://en.wikipedia.org/wiki/CAP_theorem)은 시스템의 Trade-off에 대하여 얘기할 때 고려되는 것입니다. 

Consistency는 모든 read는 가장 최신의 write나 error를 받는다는 것입니다. 어느 DB 노드에 연결하건 모든 클라이언트가 같은 데이터를 동시에 본다는 의미입니다.

Availability는 받은 모든 request에 대하여 response를 한다는 것입니다. 이 때, 가장 최신의 데이터라는 보장을 하지는 않습니다.

Partition tolerance는 노드 간의 연결에 문제가 생겨도 시스템이 작동하는 것을 말합니다. 분산형 DB에서 이를 뺄 수 없으므로 결국 Consistency와 Availability 중 고르게 됩니다. 이를 반영해 [PACELC design principle](https://en.wikipedia.org/wiki/PACELC_design_principle)에서는 Partition이 있을 경우 A와 C를 고르고 아니면(Else) Latency와 Consistency의 손상 중 골라야 한다고 합니다.

완벽한 Consistency가 어려운 분산형 시스템 전반의 특성으로 인해, 결과적으로 consistent하게 된다는[eventual consistency](https://en.wikipedia.org/wiki/Eventual_consistency)가 들어간 BASE(Basically available, soft-state, eventual consistency) consistency가 목표가 되는 경우도 많습니다. Eventual consistency는 분산형 시스템 전반에서 높은 availability를 달성하기 위해 사용되곤 합니다.

Eventual consistency의 경우 [replica간의 reconciliation](https://en.wikipedia.org/wiki/Eventual_consistency#Conflict_resolution)을 함으로 달성되는데, 다음과 같음 3가지 경우에 스케쥴이 될 수 있습니다:

- Read repair: Read가 inconsistency를 발견 시 correction 진행. Read가 느려짐
- Write repair: Write시 correction이 진행. Write가 느려짐.
- Asynchronous repair: Read와 Write외 별도로 correction이 진행됨. Merkle Tree를 사용한 방법 등이 대표적입니다. Git도 그래프형인 Merkle DAG(Directed Acyclic Graph, i.e, 다시 자기 노드로 안 돌아오는 그래프)를 사용해 변화를 track합니다.

Strong eventual consistency의 경우 서로 다른 DB 노드가 같은 업데이트를 받는다면 같은 state이다는 보장입니다.

## Read-only Replica

기본적으로는 Read만 되는 DB의 복제본입니다. Read 작업을 메인 DB 서버를 복사 또는 물리적으로 연결한 replica에서 처리할 수 있습니다.

- Byte 단위로 따온 Physical replica의 경우 주 DB가 죽으면 Failover로 메인 DB로 승격되는데도 stand-by로 쓰일 수 있습니다. 
- Logical replica의 경우 publisher(메인 노드)에 subscription을 걸고 복사 받아옵니다. 다른 작업용 테이블 추가도 가능하지만 failover시 사용하기는 힘듭니다.

글로벌 뉴스처럼 read는 많은데 write은 별로 없는 경우, Single-leader로 설정 후 replica를 지역마다 두어 read latency를 줄일 수 있습니다.

## Sharding

[DB Sharding](https://aws.amazon.com/what-is/database-sharding/)은 대규모 DB를 여러 기기에 저장하는 것 입니다. Horizontal partitioning이라 불리기도 했으며, 거대한 데이터를 더 효율적으로 관리하거나 먼 지역에서 그 지역의 write 등을 따로 관리하도록 할 수 있습니다.

같은 데이터를 다른 기기에 저장하는 DB replication과는 다른 개념입니다. Replication은 record를 노드들에 복사하고 Sharding은 다른 record를 관리입니다.

또 [normalization이나 vertical partitioning하고도 다릅니다](https://en.wikipedia.org/wiki/Shard_(database_architecture)#Database_architecture). Sharding은 row를 나누고 normalization과 vertical partitioning은 column을 나눕니다.

PostgreSQL에서 [Citus](https://www.citusdata.com/) extension등을 사용해 구현할 수 있습니다.

## DB replication

[DB replication](https://en.wikipedia.org/wiki/Replication_(computing)#Database_replication)은 같은 데이터를 여러 기기에 저장하는 것입니다. Single-leader, multi-leader, leaderless replication으로 나뉩니다.

### Single-leader Replication 구조

기본적으로 디폴트 PostgreSQL 등이 Single Leader입니다. 하나의 노드가 leader(i.e, write 작업을 받음)를 하는 구조입니다. 이 때 Read-Only Replica 등을 배치해서 failover시에 사용(Physical Replica의 경우 바이트 단위로 DB를 그대로 따라가므로 스냅샹/이미지 용도x), 먼 지역에서의 read latency 문제 헤결 등을 할 수 있습니다.

### Multi-leader Replication 구조

Active-Active 혹은 [Multi-master](https://en.wikipedia.org/wiki/Multi-master_replication)라고도 불립니다. 여러 leader가 write을 받습니다. 그 후 다른 서버로 propagate 합니다. 

각 leader역시 read-only replica 등을 가질 수 있습니다.

Synchronous한 replication의 경우 conlict 방지를 하며, Asynchronous한 경우 conflict resolution으로 차후 convergence를 위해 reconciliation을 진행합니다. 

장점으로는:
- Availability: 마스터 하나가 죽어도 다른 마스터들이 DB 유지
- 분산된 Access: 마스터(리더)가 여러 장소에 존재 가능.

단점으로는:
- Consistency: 대부분의 multi-leader replication 시스템의 경우 loosley consistent하며 ACID를 위배함
- 성능: Eager(Synchronous) replication의 경우 통신으로 인한 latency가 늘고 complexity가 증가함
- Integrity: Conflict resolution이 노드가 많아지고 latency가 커질 수록 추적이 어려워짐

데이터 센터가 여러 개 있는 경우 등에 사용됩니다. 

PostgreSQL에서는 [pgEdge](https://www.pgedge.com/) extension등을 통해 구현할 수 있습니다(EDB Postgres의 경우 오픈소스 아님). 

### Leaderless Replication 구조

[Leaderless replication](https://www.systemdesign.academy/glossary/leaderless-replication)은 하나의 primary 노드가 없고 모든 replica가 read와 write을 받을 수 있습니다.

클라이언트는 여러 노드에서 Read/Write 등을 진행합니다. ACID의 경우 D이외에는 잘 지켜지지 않습니다. 트랜잭션 자체가 많은 경우 존재하지 않습니다.. A와 I는 애초에 일반적인 트랜젝션이 없고 C의 경우 quorum이 완벽히 보장하지 못 합니다. 

Quorum Consistency의 경우 총 노드의 수 N, W의 경우 write의 성공하기 위해 acknowledge를 해야 하는 node의 수, R의 경우 read가 return하기 위해 응답해야 하는 노드의 수입니다. 

W + R > N인 경우, Read가 최신의 데이터를 가져오며 N=3인 경우 R=2, W=2인 경우가 예시입니다. W와 R을 조절하여 트레이드오프를 조절할 수 있습니다.

데이터 간의 consistency 보장을 위해 앞서 말한 read-repair와 Merkle Tree를 사용한 Anti-Entropy 백그라운드 프로세스가 사용됩니다

Dynamo, Cassandra, ScyllaDB 등이 해당 구조의 DB입니다. PostgreSQL의 경우 leaderless replication을 지원하는 extension이 없어 보입니다.

## 본연구 주제

이번에는 분산형 DB에 대한 본격적인 연구를 하기 전에 그 개념과 예시에 대해 정리해 보았습니다. 현재 관심이 가는 주제는 다음과 같습니다:

- 아주 latency가 클 수 밖에 없다면 어떻게 해야할까? 화성이나 그 너머까지 간다면 통신에만 몇 분~몇 시간 걸린다면 지구에서의 분산형 DB의 형태를 유지할 수 있을까. 노드간의 conflict 해결엔 현재 분산형 DB에서 쓰는 방법 외에 무엇이 필요할까
- Multi-leader나 leaderless의 경우 아주 장기간 DB 간의 연결이 끊겼다면 통신이 복구 시 어떻게 data reconciliation을 진행할까
- PostgreSQL에서 Citus, pgEdge 등의 extension을 통해서 sharding이나 multi-leader를 구현 가능한데 왜 leaderless는 안 보일까. RDBMS는 구조상 불가능한 것일까.

## 링크

본 선행 조사를 포함한 [GitHub repo](https://github.com/kosmostree/mini-researches)의 `/exploratory-surveys/replicas-and-distributed-db`에서 찾을 수 있으며 본 연구가 진행이 된다면 해당 디렉토리의 리소스로 옮겨질 예정입니다.
