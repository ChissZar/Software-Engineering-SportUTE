package vn.edu.ute.sportute.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleSummaryDTO {
    private String id;
    private String name;
    private String description;
    private String badgeLabel;
    private String colorClass;
    private int featureCount;
    private int totalFeatures;
    private int featurePercent;
    private int accountCount;
}
