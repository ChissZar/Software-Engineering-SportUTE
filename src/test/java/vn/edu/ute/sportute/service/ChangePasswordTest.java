package vn.edu.ute.sportute.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.edu.ute.sportute.entity.Account;
import vn.edu.ute.sportute.repository.*;
import vn.edu.ute.sportute.service.impl.AuthServiceImpl;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChangePasswordTest {
    private final AccountRepository accounts=mock(AccountRepository.class);
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    private final Account account=Account.builder().id("TK005").username("minhanh")
            .password(encoder.encode("OldPass123!")).build();
    private final AuthServiceImpl service=new AuthServiceImpl(accounts,mock(CustomerRepository.class),mock(RoleRepository.class),encoder);

    private void existingAccount() {
        when(accounts.findByUsernameOrEmail("minhanh","minhanh")).thenReturn(Optional.of(account));
    }
    @Test void changesOnlyAuthenticatedAccountAndStoresHash() {
        existingAccount();
        service.changePassword("minhanh","OldPass123!","NewPass456!","NewPass456!");
        assertTrue(encoder.matches("NewPass456!",account.getPassword()));
        assertFalse(encoder.matches("OldPass123!",account.getPassword()));
        assertNotEquals("NewPass456!",account.getPassword());
        verify(accounts).save(account);
    }
    @Test void wrongCurrentPasswordDoesNotWrite() {
        existingAccount();String original=account.getPassword();
        assertThrows(IllegalArgumentException.class,()->service.changePassword("minhanh","wrong","NewPass456!","NewPass456!"));
        assertEquals(original,account.getPassword());verify(accounts,never()).save(any());
    }
    @Test void rejectsMismatchShortSameAndOversizedUnicodePassword() {
        existingAccount();String original=account.getPassword();
        for(String[] pair:new String[][]{{"NewPass456!","different"},{"short","short"},{"OldPass123!","OldPass123!"},{"ậ".repeat(25),"ậ".repeat(25)}})
            assertThrows(IllegalArgumentException.class,()->service.changePassword("minhanh","OldPass123!",pair[0],pair[1]));
        assertEquals(original,account.getPassword());verify(accounts,never()).save(any());
    }
    @Test void missingIdentityDoesNotWrite() {
        assertThrows(IllegalArgumentException.class,()->service.changePassword(null,"old","NewPass456!","NewPass456!"));
        assertThrows(IllegalArgumentException.class,()->service.changePassword("missing","old","NewPass456!","NewPass456!"));
        verify(accounts,never()).save(any());
    }
}
