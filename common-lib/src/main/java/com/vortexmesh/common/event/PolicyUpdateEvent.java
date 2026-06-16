package com.vortexmesh.common.event;

import lombok.*;
import com.vortexmesh.common.dto.PolicyDTO;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyUpdateEvent {
    private String eventId;
    private String serviceId;
    private PolicyDTO policy;
    private ActionType action;
    private Instant timestamp;

    public enum ActionType {
        CREATE, UPDATE, DELETE
    }
}
