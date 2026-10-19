package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.response.RoleSummaryDTO;
import vn.edu.ute.sportute.dto.response.UserAccountDTO;
import vn.edu.ute.sportute.entity.Account;
import vn.edu.ute.sportute.entity.Feature;
import vn.edu.ute.sportute.entity.Role;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PermissionService {

    List<Role> getAllRoles();

    List<RoleSummaryDTO> getRoleSummaries();

    List<Feature> getAllFeatures();

    List<Account> getAllAccounts();

    List<UserAccountDTO> getAllUserAccountDTOs();

    Map<String, Set<String>> getRoleFeatureMatrix();

    void updateRoleFeatures(String roleId, List<String> featureIds);

    void saveFullMatrix(Map<String, List<String>> matrix);

    void resetMatrixToDefault();

    void updateAccountRoles(String accountId, List<String> roleIds);

    String toggleAccountStatus(String accountId);

    void createRole(String id, String name, String description);

    Map<String, Object> getPermissionStatistics();
}
