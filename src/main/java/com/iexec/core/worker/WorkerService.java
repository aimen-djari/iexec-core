package com.iexec.core.worker;

import com.iexec.core.configuration.WorkerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.iexec.core.utils.DateTimeUtils.addMinutesToDate;

@Slf4j
@Service
public class WorkerService {

    private WorkerRepository workerRepository;
    private WorkerConfiguration workerConfiguration;

    public WorkerService(WorkerRepository workerRepository,
                         WorkerConfiguration workerConfiguration) {
        this.workerRepository = workerRepository;
        this.workerConfiguration = workerConfiguration;
    }

    public Optional<Worker> getWorker(String walletAddress) {
        return workerRepository.findByWalletAddress(walletAddress);
    }

    public Worker addWorker(Worker worker) {
        Optional<Worker> oWorker = workerRepository.findByWalletAddress(worker.getWalletAddress());

        if (oWorker.isPresent()) {
            Worker existingWorker = oWorker.get();
            log.info("The worker is already registered [workerId:{}]", existingWorker.getId());
            worker.setId(existingWorker.getId());
            worker.setParticipatingChainTaskIds(existingWorker.getParticipatingChainTaskIds());
            worker.setComputingChainTaskIds(existingWorker.getComputingChainTaskIds());
        } else {
            log.info("Registering new worker");
        }

        return workerRepository.save(worker);
    }

    public boolean isAllowedToJoin(String workerAddress){
        List<String> whitelist = workerConfiguration.getWhitelist();
        // if the whitelist is empty, there is no restriction on the workers
        if (whitelist.isEmpty()){
            return true;
        }
        return whitelist.contains(workerAddress);
    }

    public Optional<Worker> updateLastAlive(String walletAddress) {
        Optional<Worker> optional = workerRepository.findByWalletAddress(walletAddress);
        if (optional.isPresent()) {
            Worker worker = optional.get();
            worker.setLastAliveDate(new Date());
            workerRepository.save(worker);
            return Optional.of(worker);
        }

        return Optional.empty();
    }

    public boolean isWorkerAllowedToAskReplicate(String walletAddress) {
        Optional<Date> oDate = getLastReplicateDemand(walletAddress);
        if (!oDate.isPresent()) {
            return true;
        }

        // the difference between now and the last time the worker asked for work should be less than the period allowed
        // in the configuration (500ms since (now - lastAsk) can still be slightly too small even if the worker behave nicely)
        long now = new Date().getTime();
        long lastAsk = oDate.get().getTime();

        return (now - lastAsk) + 500 > workerConfiguration.getAskForReplicatePeriod();
    }

    public Optional<Date> getLastReplicateDemand(String walletAddress) {
        Optional<Worker> optional = workerRepository.findByWalletAddress(walletAddress);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        Worker worker = optional.get();

        return Optional.ofNullable(worker.getLastReplicateDemandDate());
    }

    public Optional<Worker> updateLastReplicateDemand(String walletAddress) {
        Optional<Worker> optional = workerRepository.findByWalletAddress(walletAddress);
        if (optional.isPresent()) {
            Worker worker = optional.get();
            worker.setLastReplicateDemandDate(new Date());
            workerRepository.save(worker);
            return Optional.of(worker);
        }

        return Optional.empty();
    }

    public Optional<Worker> addChainTaskIdToWorker(String chainTaskId, String walletAddress) {
        Optional<Worker> optional = workerRepository.findByWalletAddress(walletAddress);
        if (optional.isPresent()) {
            Worker worker = optional.get();
            worker.addChainTaskId(chainTaskId);
            log.info("Added chainTaskId to worker [chainTaskId:{}, workerName:{}]", chainTaskId, walletAddress);
            return Optional.of(workerRepository.save(worker));
        }
        return Optional.empty();
    }

    public List<String> getChainTaskIds(String walletAddress) {
        Optional<Worker> optional = workerRepository.findByWalletAddress(walletAddress);
        if (optional.isPresent()) {
            Worker worker = optional.get();
            return worker.getParticipatingChainTaskIds();
        }
        return Collections.emptyList();
    }

    public Optional<Worker> removeChainTaskIdFromWorker(String chainTaskId, String walletAddress) {
        Optional<Worker> optional = workerRepository.findByWalletAddress(walletAddress);
        if (optional.isPresent()) {
            Worker worker = optional.get();
            worker.removeChainTaskId(chainTaskId);
            log.info("Removed chainTaskId from worker [chainTaskId:{}, walletAddress:{}]", chainTaskId, walletAddress);
            return Optional.of(workerRepository.save(worker));
        }
        return Optional.empty();
    }

    public Optional<Worker> removeComputedChainTaskIdFromWorker(String chainTaskId, String walletAddress) {
        Optional<Worker> optional = workerRepository.findByWalletAddress(walletAddress);
        if (optional.isPresent()) {
            Worker worker = optional.get();
            worker.removeComputedChainTaskId(chainTaskId);
            log.info("Removed computed chainTaskId from worker [chainTaskId:{}, walletAddress:{}]", chainTaskId, walletAddress);
            return Optional.of(workerRepository.save(worker));
        }
        return Optional.empty();
    }


    // worker is considered lost if it didn't ping for 1 minute
    public List<Worker> getLostWorkers() {
        Date oneMinuteAgo = addMinutesToDate(new Date(), -1);
        return workerRepository.findByLastAliveDateBefore(oneMinuteAgo);
    }

    // worker is considered alive if it ping after 1 minute
    public List<Worker> getAliveWorkers() {
        Date oneMinuteAgo = addMinutesToDate(new Date(), -1);
        return workerRepository.findByLastAliveDateAfter(oneMinuteAgo);
    }

    public boolean canAcceptMoreWorks(String walletAddress) {
        Optional<Worker> optionalWorker = getWorker(walletAddress);
        if (!optionalWorker.isPresent()){
            return false;
        }

        Worker worker = optionalWorker.get();
        int workerCpuNb = worker.getCpuNb();
        int runningReplicateNb = worker.getComputingChainTaskIds().size();

        if (runningReplicateNb >= workerCpuNb) {
            log.info("Worker asking for too many replicates [walletAddress: {}, runningReplicateNb:{}, workerCpuNb:{}]",
                    walletAddress, runningReplicateNb, workerCpuNb);
            return false;
        }

        return true;
    }

    public int getAliveAvailableCpu() {
        int availableCpus = 0;
        for (Worker worker: getAliveWorkers()){
            int workerCpuNb = worker.getCpuNb();
            int computingReplicateNb = worker.getComputingChainTaskIds().size();
            int availableCpu = workerCpuNb - computingReplicateNb;
            availableCpus+= availableCpu;
        }
        return availableCpus;
    }

    public int getAliveTotalCpu() {
        int totalCpus = 0;
        for (Worker worker: getAliveWorkers()){
            totalCpus+= worker.getCpuNb();
        }
        return totalCpus;
    }
}
