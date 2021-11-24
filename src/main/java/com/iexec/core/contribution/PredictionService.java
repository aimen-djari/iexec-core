/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.core.contribution;

import com.iexec.core.replicate.Replicate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class PredictionService {

    private final ContributionService contributionService;

    public PredictionService(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    Prediction getContributedBestPrediction(List<Replicate> replicates) {
        Set<String> distinctContributions = contributionService.getDistinctContributions(replicates);
        Prediction bestPrediction = Prediction.builder().contribution("").weight(0).build();

        for (String predictionContribution : distinctContributions) {
            int predictionWeight = contributionService.getContributedWeight(
                    replicates,
                    predictionContribution
            );

            if (predictionWeight >= bestPrediction.getWeight()) {
                bestPrediction.setContribution(predictionContribution);
                bestPrediction.setWeight(predictionWeight);
            }
        }
        return bestPrediction;
    }

    private int getContributedBestPredictionWeight(List<Replicate> replicates) {
        return this.getContributedBestPrediction(replicates).getWeight();
    }

    /*
     *
     * Considering pending workers are going to contribute to the best prediction
     * Counting pending and contributed
     *
     * */
    int getBestPredictionWeight(List<Replicate> replicates, long maxExecutionTime) {
        int contributedBestPredictionWeight = getContributedBestPredictionWeight(replicates);
        int pendingWeight = contributionService.getPendingWeight(replicates, maxExecutionTime);

        int bestPredictionWeight;
        if (pendingWeight == 0 && contributedBestPredictionWeight == 0) {
            bestPredictionWeight = 0;
        } else if (pendingWeight > 0 && contributedBestPredictionWeight == 0) {
            bestPredictionWeight = pendingWeight;
        } else if (pendingWeight == 0 && contributedBestPredictionWeight > 0) {
            bestPredictionWeight = contributedBestPredictionWeight;
        } else {
            bestPredictionWeight = contributedBestPredictionWeight * pendingWeight;
        }
        return bestPredictionWeight;
    }

    /*
     *
     * Sum all prediction weights but exclude contributed best prediction weight
     *
     * */
    int getWorstPredictionsWeight(List<Replicate> replicates) {
        Set<String> distinctContributions = contributionService.getDistinctContributions(replicates);
        String bestPredictionContribution = this.getContributedBestPrediction(replicates).getContribution();

        int allOtherPredictionsWeight = 0;

        for (String contribution : distinctContributions) {
            int predictionWeight = contributionService.getContributedWeight(replicates, contribution);

            if (!contribution.equals(bestPredictionContribution)) {
                allOtherPredictionsWeight = allOtherPredictionsWeight + predictionWeight;
            }
        }
        return allOtherPredictionsWeight;
    }

}