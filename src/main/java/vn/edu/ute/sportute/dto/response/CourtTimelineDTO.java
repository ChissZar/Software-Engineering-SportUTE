package vn.edu.ute.sportute.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class CourtTimelineDTO {
    private String courtId;
    private String courtName;
    private String sportId;
    private String sportName;
    private String courtStatus;
    
    @Builder.Default
    private List<TimelineSlotDTO> slots = new ArrayList<>();

    public CourtTimelineDTO() {
        this.slots = new ArrayList<>();
    }

    public CourtTimelineDTO(String courtId, String courtName, String sportId, String sportName, String courtStatus, List<TimelineSlotDTO> slots) {
        this.courtId = courtId;
        this.courtName = courtName;
        this.sportId = sportId;
        this.sportName = sportName;
        this.courtStatus = courtStatus;
        this.slots = slots != null ? slots : new ArrayList<>();
    }
    public static class CourtTimelineDTOBuilder {
        public CourtTimelineDTOBuilder() {
        }
    }
}
