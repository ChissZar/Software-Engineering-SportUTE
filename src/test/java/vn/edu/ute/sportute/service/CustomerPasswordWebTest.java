package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.ute.sportute.config.SecurityConfig;
import vn.edu.ute.sportute.controller.CustomerPasswordController;
import vn.edu.ute.sportute.security.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=CustomerPasswordController.class,
    excludeFilters=@org.springframework.context.annotation.ComponentScan.Filter(
        type=org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes=vn.edu.ute.sportute.config.GlobalUserAdvice.class))
@Import(SecurityConfig.class)
class CustomerPasswordWebTest {
    @Autowired MockMvc mvc;
    @MockBean AuthService auth;
    @MockBean CustomAuthenticationSuccessHandler successHandler;
    @MockBean CustomAuthenticationFailureHandler failureHandler;

    @Test void usesSessionIdentityAndRejectsMissingCsrfOrWrongRole() throws Exception {
        mvc.perform(post("/customer/password/change").with(user("minhanh").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/customer/password/change").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/customer/password/change").with(csrf()))
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(auth);
        mvc.perform(post("/customer/password/change").with(user("minhanh").roles("CUSTOMER")).with(csrf())
                .param("username","someone-else").param("currentPassword","old").param("newPassword","NewPass123!").param("confirmPassword","NewPass123!"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(auth).changePassword("minhanh","old","NewPass123!","NewPass123!");
    }
    @Test void validationErrorIsNotReportedAsSuccess() throws Exception {
        doThrow(new IllegalArgumentException("Mật khẩu hiện tại không đúng")).when(auth).changePassword(anyString(),anyString(),anyString(),anyString());
        mvc.perform(post("/customer/password/change").with(user("minhanh").roles("CUSTOMER")).with(csrf())
                .param("currentPassword","wrong").param("newPassword","NewPass123!").param("confirmPassword","NewPass123!"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }
}
