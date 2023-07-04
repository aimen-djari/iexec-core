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

package com.iexec.core.task.event;

import com.iexec.common.contract.generated.IexecHubContract;
import com.iexec.common.utils.BytesUtils;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Getter
@NoArgsConstructor
@Builder
public class TaskInterruptedEvent {

	private String chainTaskId;
	private BigInteger blockNumber;
	private BigInteger duration;

	public TaskInterruptedEvent(String chainTaskId, BigInteger blockNumber, BigInteger duration) {
		this.chainTaskId = chainTaskId;
		this.blockNumber = blockNumber;
		this.duration = duration;
	}

	public TaskInterruptedEvent(IexecHubContract.TaskInterruptEventResponse taskInterruptedEventResponse) {
		this.chainTaskId = BytesUtils.bytesToString(taskInterruptedEventResponse.taskid);
		this.blockNumber = taskInterruptedEventResponse.log.getBlockNumber();
		this.duration = taskInterruptedEventResponse.finalDuration;
	}

}
