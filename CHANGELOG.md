# Changelog

All notable changes to this project will be documented in this file.

## [[8.1.2]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v8.1.2) 2023-06-29

## Bug fixes
- Prevent race conditions in `WorkerService`. (#602)
### Dependency Upgrades
- Upgrade to `iexec-commons-poco` 3.0.5. (#602)

## [[8.1.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v8.1.1) 2023-06-23

### Dependency Upgrades
- Upgrade to `iexec-common` 8.2.1. (#599)
- Upgrade to `iexec-commons-poco` 3.0.4. (#599)
- Upgrade to `iexec-blockchain-adapter-api-library` 8.1.1. (#599)
- Upgrade to `iexec-result-proxy-library` 8.1.1. (#599)
- Upgrade to `iexec-sms-library` 8.1.1. (#599)

## [[8.1.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v8.1.0) 2023-06-09

### New Features
- Add ContributeAndFinalize to `ReplicateWorkflow`. (#574)
- Add check for ContributeAndFinalize in `ReplicatesService`. (#576 #582)
- Add `running2Finalized2Completed` in `TaskUpdateManager`. (#577 #578)
- Disable `contributeAndFinalize` with CallBack. (#579 #581)
- Add purge cached task descriptions ability. (#587)
- Add detectors for `ContributeAndFinalize` flow. (#590 #593)
### Bug Fixes
- Prevent race condition on replicate update. (#568)
- Use builders in test classes. (#589)
### Quality
- Remove unused methods in `IexecHubService`. (#572)
- Clean unused Replicate methods and update tests. (#573)
- Clean unused `ReplicateStatus#RESULT_UPLOAD_REQUEST_FAILED`. (#575)
- Refactor unnotified detectors to avoid code duplication. (#580)
- Use `==` or `!=` operators to test the equality of enums. (#584)
- Rearrange checks order to avoid call to database. (#585)
- Move methods to get event blocks from `iexec-commons-poco`. (#588)
- Rename detectors' methods and fields to match Ongoing/Done standard. (#591)
### Dependency Upgrades
- Upgrade to `iexec-common` 8.2.0. (#571 #575 #586 #594)
- Add new `iexec-commons-poco` 3.0.2 dependency. (#571 #574 #586 #587 #588 #592 #594)
- Upgrade to `iexec-blockchain-adapter-api-library` 8.1.0. (#594)
- Upgrade to `iexec-result-proxy-library` 8.1.0. (#594)
- Upgrade to `iexec-sms-library` 8.1.0. (#594)

## [[8.0.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v8.0.1) 2023-03-20

### Bug Fixes
- Remove explicit version on `micrometer-registry-prometheus` dependency. (#563)
- Send a `TaskNotificationType` to worker with a 2XX HTTP status code. (#564)
- Remove `com.iexec.core.dataset` package. (#565)
- Improve log on `canUpdateReplicateStatus` method. (#566)

## [[8.0.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v8.0.0) 2023-03-08

### New Features
* Support Gramine framework for TEE tasks.
* Retrieve location of SMS services through an _iExec Platform Registry_.
* Improve authentication on scheduler.
  * burn challenge after login.
  * handle JWT expiration through the expiration claim.
  * cache JWT until expiration.
  * better claims usage.
* Show application version on banner.
### Bug Fixes
* Always return a `TaskNotificationType` on replicate status update when it has been authorized.
* Handle task added twice.
### Quality
* Improve code quality and tests.
* Removed unused variables in configuration.
* Use existing `toString()` method to serialize and hash scheduler public configuration.
* Use recommended annotation in `MetricController`.
* Remove `spring-cloud-starter-openfeign` dependency.
### Dependency Upgrades
* Replace the deprecated `openjdk` Docker base image with `eclipse-temurin` and upgrade to Java 11.0.18 patch.
* Upgrade to Spring Boot 2.6.14.
* Upgrade to Gradle 7.6.
* Upgrade OkHttp to 4.9.0.
* Upgrade `jjwt` to `jjwt-api` 0.11.5.
* Upgrade to `iexec-common` 7.0.0.
* Upgrade to `jenkins-library` 2.4.0.

## [[7.3.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v7.3.1) 2023-02-17

* Subscribe only to deal events targeting a specific workerpool.

## [[7.3.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v7.3.0) 2022-12-18

* Add endpoint to allow health checks.

## [[7.2.2]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v7.2.2) 2022-12-20

* Use `iexec-common` version [6.2.0](https://github.com/iExecBlockchainComputing/iexec-common/releases/tag/v6.2.0).
* Use `okhttp` version 4.9.0 to keep it consistent with the one in the web3j dependency imported by `iexec-common`.

## [[7.2.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v7.2.1) 2022-12-13

* Replace `sessionId` implementation with a hash of the public configuration. From a consumer point of view, a constant hash received from the `POST /ping` response indicates that the scheduler configuration has not changed. With such constant hash, either the scheduler has restarted or not, the consumer does not need to reboot.

## [[7.2.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v7.2.0) 2022-12-09

* Increments of jenkins-library up to version 2.2.3. Enable SonarCloud analyses on branches and Pull Requests.
* Update `iexec-common` version to [6.1.0](https://github.com/iExecBlockchainComputing/iexec-common/releases/tag/v6.1.0).

## [[7.1.3]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v7.1.3) 2022-12-07

* Bump version to 7.1.3.

## [[7.1.2]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v7.1.2) 2022-12-07

* Update README and add CHANGELOG.

## [[7.1.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v7.1.1) 2022-11-28

* Fix build process.

## [[7.1.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v7.1.0) 2022-08-11

* Retrieve a task execution status. Logs can only be retrieved by the person who requested the execution.
* Use OpenFeign client libraries.
* Fix concurrency issues.
* Use Spring Boot 2.6.2.
* Use Java 11.0.15.

## [[7.0.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v7.0.1) 2022-01-05

* Reduce probability of giving tasks whose consensus is already reached to additional workers.
* Remove useless logs.
* Handle task supply and task update management based on their level of priority.
* Upgrade automated build system.

## [[7.0.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/7.0.0) 2021-12-17

Highly improved throughput of the iExec protocol.

What has changed since v6.0.0?
* Fix task status deadlock. Chances to reach RUNNING task status for given states of replicates are now increased.
* Fix race condition on replicate attribution.
* Upgrade Jacoco/Sonarqube reporting and plugins.
* Consume blockchain configuration on iexec-blockchain-adapter-api & expose its URL for iexec-worker
* Upgrade artifacts publishing
* Enable local import of iexec-common
* Upgrade to Gradle 6.8.3
* Upgrade to JUnit5
* Fix concurrent upload requests.
* Merge abort notifications into a single notification with custom abort cause.
* Send transactions over a dedicated blockchain adapter micro-service.
* Reply gracefully to the worker when a status is already reported.
* Abort a TEE task when all alive workers did fail to run it.
* Move task state machine to a dedicated service.
* Remove useless internal task statuses belonging to the result upload request stage.
* Fix Recovering for the Retryable updateReplicateStatus(..) method.
* Add checks before locally upgrading to the INITIALIZED status
* Remove 2-blocks waiting time before supplying a new replicate
* Fix OptimisticLockingFailureException happening when 2 detectors detect the same change at the same time, leading to race updates on a same task
* Reuse socket when sending multiple requests to a blockchain node.
* Replay fromBlockNumber now lives in a dedicated configuration:
  A configuration document did store two different states. Updates on different states at the same time might lead to race conditions when saving to database. Now each state has its own document to avoid race conditions when saving.
* Fix TaskRepositoy.findByChainDealIdAndTaskIndex() to return unique result.

## [[6.4.2]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.4.2) 2021-12-14

* Fix task status deadlock. Chances to reach RUNNING task status for given states of replicates are now increased.

## [[6.4.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.4.1) 2021-11-30

* Fix race condition on replicate attribution.
* Upgrade Jacoco/Sonarqube reporting and plugins.

## [[6.4.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.4.0) 2021-11-25

* Consume blockchain configuration on iexec-blockchain-adapter-api & expose its URL for iexec-worker.
* Upgrade artifacts publishing.
* Enable local import of iexec-common.
* Upgrade to Gradle 6.8.3.
* Upgrade to JUnit5.

## [[6.3.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.3.0) 2021-11-16

* Fix concurrent upload requests.
* Merge abort notifications into a single notification with custom abort cause.

## [[6.2.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.2.0) 2021-11-05

* Send transactions over a dedicated blockchain adapter micro-service.
* Reply gracefully to the worker when a status is already reported.
* Abort a TEE task when all alive workers did fail to run it.
* Moved task state machine to a dedicated service.
* Removed useless internal task statuses belonging to the result upload request stage.

## [[6.1.6]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.1.6) 2021-10-21

* Fixed Recovering for the Retryable updateReplicateStatus(..) method.

## [[6.1.5]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.1.5) 2021-10-21

* Added checks before locally upgrading to the INITIALIZED status.
* Removed 2-blocks waiting time before supplying a new replicate.

## [[6.1.4]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.1.4) 2021-10-14

* Fixed OptimisticLockingFailureException happening when 2 detectors detect the same change at the same time, leading to race updates on a same task.

## [[6.1.3]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.1.3) 2021-10-05

* Bump iexec-common dependency (iexec-common@5.5.1) featuring socket reuse when sending multiple requests to a blockchain node.

## [[6.1.2]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.1.2) 2021-10-01

Bugfix - Replay fromBlockNumber now lives in a dedicated configuration:
* A configuration document did store two different states. Updates on different states at the same time might lead to race conditions when saving to database. Now each state has its own document to avoid race conditions when saving.

## [[6.0.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/6.0.1) 2021-09-28

* Fix : TaskRepositoy.findByChainDealIdAndTaskIndex() returns non-unique result

## [[6.0.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/v6.0.0) 2021-07-29

What's new?
* Added Prometheus actuators
* Moved TEE workflow configuration to dedicated service

## [[5.1.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/5.1.1) 2021-04-12

What is patched?
* Updated management port and actuator endpoints.

## [[5.1.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/5.1.0) 2021-03-26

What's new?

Fix WebSockets problem:
* Enhance different task stages detection. When detecting unnotified contribute/reveal,
  we use the initialization block instead of the current block to lookup for the contribute/reveal metadata.
* Use a dedicated TaskScheduler for STOMP WebSocket heartbeats.
* Use a dedicated TaskScheduler for @Scheduled tasks.
* Use a dedicated TaskExecutor for @Async tasks.
* TaskService is now the entry point to update tasks.
* feature/task-replay.

Also:
* Use the deal block as a landmark for a task.
* Keep the computing task list consistent.
* Worker should be instructed to contribute whenever it is possible (e.g. app/data download failure).
* Enhance worker lost detection.
* Enhance final deadline detection for tasks.

## [[5.0.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/5.0.0) 2020-07-22

What's new?
* Dapp developers can browse worker computing logs over iexec-core API.
* Task result link is standardized and generic. It supports Ethereum, IPFS & Dropbox "storage" providers.
* Result storage feature is moved to new [iExec Result Proxy](https://github.com/iExecBlockchainComputing/iexec-result-proxy).
* Full compatibility with new [iExec Secret Management Service](https://github.com/iExecBlockchainComputing/iexec-sms).
* Compatibility with latest PoCo 5.1.0 smart contracts.

## [[4.0.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/4.0.1) 2020-02-25

What's fixed?
* More resistance to unsync Ethereum nodes.

## [[4.0.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/4.0.0) 2019-12-13

What's new?
* Native-token sidechain compatibility.
* GPU workers support.
* Log aggregation.

What's fixed?
* Database indexes.
* JWT/challenge validity duration.
* Worker freed after contribution timeout.

## [[3.2.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/3.2.0) 2019-09-17

What is new?
* Bag Of Tasks (BoT): Bag Of Tasks Deals can now be processed by the middleware.
* Use of iexec input files: Sometimes external resources are needed to run a computation without using a dataset, that is what input files are for.
* Whitelisting: Now the core can define a whitelist of workers that are allowed to connect to the pool.
* Https: Workers can connect to the core using https.

What is patched?
* The project has been updated to java 11 and web3j 4.3.0.
* Internal refactoring to handle replicates update better.
* Limit workers that ask for replicates too often.
* Update workers configuration when they disconnect/reconnect.

## [[3.1.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/3.1.0) 2019-07-11

What's new?
* Full end-to-end encryption inside a Trusted Execution Environment (TEE) powered by Intel(R) SGX.
* Implemented the Proof-of-Contribution (PoCo) Sarmenta's formula for a greater task dispatching.

What's patched?
* Reopen task worflow is back.
* A single FAILED replicate status when a completion is impossible.
* WORKER_LOST is not set for replicates which are already FAILED.
* Restart when ethereum node is not available at start-up.

## [[3.0.1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/3.0.1) 2019-05-22

This release is a patch to fix an issue following the release of version 3.0.0.

When asking for a replicate, a worker sends the latest available block number from the node it is is connected to.
If that node is a little bit behind the node the core is connected to, the worker will have a disadvantage compare to workers connected to more up-to-date nodes.
To avoid this disadvantage, now the core waits for a few blocks before sending replicates to workers.

## [[3.0.0]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/3.0.0) 2019-05-15

This release contains the core set of changes in the 3.0.0-alpha-X releases and some other features/fixes.
In summary this version introduces:
* A new architecture: the core has been completely re-architectured from the version 2.
* Latest PoCo use.
* Better management of transaction with the ethereum blockchain.
* Failover mechanisms: in case some workers are lost or restarted when working on a specific task, internal mechanisms will redistribute the task or use as much as possible the work performed by the lost / restarted workers.
* iExec End-To-End Encryption with Secret Management Service (SMS): from this version, inputs and outputs of the job can be fully encrypted using the Secret Management Service.
* Decentralized oracle: If the result is needed by a smart contract, it is available directly on the blockchain.
* IPFS: data can be retrieved from IPFS and public results can be published on IPFS.

For further information on this version, please read our documentation.

## [[3.0.0-alpha3]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/3.0.0-alpha3) 2019-04-15

* Possibility to choose between slow and fast transactions.
* Embedded IPFS node for default iexec-worker push of the results.
* Better engine for supporting iexec-worker disconnections and recovery actions.
* Brand new Continuous Integration and Delivery Pipeline.
* Enabled SockJS over HTTP WebSocket sub-protocol to bypass some proxies.

## [[3.0.0-alpha2]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/3.0.0-alpha2) 2019-02-08

* Some improvements have been made on the core:
* Some general refactoring on the detectors.
* Bug fix regarding the new PoCo version.
* The core now also checks the errors on the blockchain sent by the workers.
Enhancement regarding the result repo.
Updated PoCo chain version 3.0.21.

## [[3.0.0-alpha1]](https://github.com/iExecBlockchainComputing/iexec-core/releases/tag/3.0.0-alpha1) 2019-01-25

* This release is the first alpha release of the version 3.0.0.
